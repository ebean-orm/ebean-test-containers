package io.ebean.test.containers;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.CreateStreamRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlociContainerIntegrationTest {

  @Test
  void start_viaBuilder() {
    FlociContainer container = FlociContainer.builder("latest")
      .awsRegion("ap-southeast-2")
      .services("dynamodb,kinesis,sns,sqs,s3")
      .containerName("ut_floci_dkss2")
      .image("hectorvent/floci:latest")
      .port(4578)
      .build();

    container.startMaybe();

    AwsSDKv2 sdk = container.sdk2();
    assertThat(sdk.endpoint()).isNotNull();

    DynamoDbClient dynamoDB = sdk.dynamoDBClient();
    createTable(dynamoDB);

    KinesisClient kinesis = sdk.kinesisClient();
    SnsClient sns = sdk.snsClient();
    SqsClient sqs = sdk.sqsClient();
    S3Client s3 = sdk.s3Client();

    useSnsSqs(sdk);
    useKinesis(sdk);
    useS3(s3);

    assertThat(container.endpointUrl()).isNotNull();
    assertThat(container.awsRegion()).isEqualTo("ap-southeast-2");
    assertThat(kinesis).isNotNull();
    assertThat(sns).isNotNull();
    assertThat(sqs).isNotNull();
    assertThat(s3).isNotNull();
  }

  @Test
  void randomPort() {
    FlociContainer container = FlociContainer.builder("latest")
      .services("dynamodb")
      .port(0)
      .build();

    container.startMaybe();
    assertThat(container.port()).isGreaterThan(0);

    AwsSDKv2 sdk = container.sdk2();
    createTable(sdk.dynamoDBClient());

    container.stop();
    container.stop();
  }

  @Test
  void start() {
    FlociContainer container = FlociContainer.builder("latest")
      .services("dynamodb")
      .build();

    container.startMaybe();

    AwsSDKv2 sdk = container.sdk();
    createTable(sdk.dynamoDBClient());
  }

  private void useS3(S3Client s3Client) {
    String bucketName = "s3test-" + System.currentTimeMillis();
    CreateBucketResponse response = s3Client.createBucket(b -> b.bucket(bucketName));
    assertThat(response).isNotNull();

    ListBucketsResponse listBucketsResponse = s3Client.listBuckets();
    List<Bucket> buckets = listBucketsResponse.buckets();
    assertThat(buckets).isNotEmpty();
  }

  private void useKinesis(AwsSDKv2 sdk) {
    KinesisClient kinesis = sdk.kinesisClient();
    String streamName = "hello-stream-" + System.currentTimeMillis();
    try {
      kinesis.createStream(CreateStreamRequest.builder()
        .streamName(streamName)
        .shardCount(1)
        .build());
    } catch (software.amazon.awssdk.services.kinesis.model.ResourceInUseException e) {
      // ignored for reruns against a shared integration container
    } catch (software.amazon.awssdk.services.kinesis.model.KinesisException e) {
      if (e.statusCode() == 415) {
        // Floci currently expects JSON for Kinesis while AWS SDK v2 sends CBOR by default.
        // Fallback request validates the Kinesis service path for this integration test.
        createStreamViaJsonProtocol(sdk.endpoint(), streamName);
      } else {
        throw e;
      }
    }
  }

  private void createStreamViaJsonProtocol(URI endpoint, String streamName) {
    String payload = "{\"StreamName\":\"" + streamName + "\",\"ShardCount\":1}";
    HttpRequest request = HttpRequest.newBuilder(endpoint)
      .header("X-Amz-Target", "Kinesis_20131202.CreateStream")
      .header("Content-Type", "application/x-amz-json-1.1")
      .POST(HttpRequest.BodyPublishers.ofString(payload))
      .build();
    try {
      HttpResponse<String> response = HttpClient.newHttpClient()
        .send(request, HttpResponse.BodyHandlers.ofString());
      assertThat(response.statusCode()).isIn(200, 400);
      if (response.statusCode() == 400) {
        assertThat(response.body()).contains("ResourceInUseException");
      }
    } catch (IOException e) {
      throw new IllegalStateException("Error creating Kinesis stream via JSON fallback", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted creating Kinesis stream via JSON fallback", e);
    }
  }

  private void useSnsSqs(AwsSDKv2 sdk) {
    SqsClient sqs = sdk.sqsClient();
    SnsClient sns = sdk.snsClient();

    String suffix = String.valueOf(System.currentTimeMillis());
    String sqsName = "SQS_NAME_" + suffix;
    String snsTopicName = "SNS_TOPIC_" + suffix;

    String sqsUrl = sqs.createQueue(CreateQueueRequest.builder()
      .queueName(sqsName)
      .build()).queueUrl();

    String snsTopicArn = sns.createTopic(CreateTopicRequest.builder()
      .name(snsTopicName)
      .build()).topicArn();

    String sqsArn = sqs.getQueueAttributes(GetQueueAttributesRequest.builder()
        .queueUrl(sqsUrl)
        .attributeNames(QueueAttributeName.QUEUE_ARN)
        .build())
      .attributes().get(QueueAttributeName.QUEUE_ARN);

    sns.subscribe(SubscribeRequest.builder()
      .topicArn(snsTopicArn)
      .protocol("sqs")
      .endpoint(sqsArn)
      .build());

    sns.publish(PublishRequest.builder().topicArn(snsTopicArn).message("Hello 0").build());
    sns.publish(PublishRequest.builder().topicArn(snsTopicArn).message("Hello 1").build());
    sns.publish(PublishRequest.builder().topicArn(snsTopicArn).message("Hello 2").build());

    ReceiveMessageResponse receiveResp = sqs.receiveMessage(ReceiveMessageRequest.builder()
      .queueUrl(sqsUrl)
      .maxNumberOfMessages(2)
      .waitTimeSeconds(10)
      .build());
    List<Message> messages = receiveResp.messages();
    assertThat(messages).isNotEmpty();

    sns.deleteTopic(b -> b.topicArn(snsTopicArn));
    sqs.deleteQueue(b -> b.queueUrl(sqsUrl));
  }

  private void createTable(DynamoDbClient dynamoDB) {
    try {
      List<KeySchemaElement> keys = asList(KeySchemaElement.builder()
        .attributeName("key")
        .keyType(KeyType.HASH)
        .build());
      List<AttributeDefinition> attrs = asList(AttributeDefinition.builder()
        .attributeName("key")
        .attributeType(ScalarAttributeType.S)
        .build());

      CreateTableResponse result = dynamoDB.createTable(CreateTableRequest.builder()
        .attributeDefinitions(attrs)
        .tableName("junk-floci-" + System.currentTimeMillis())
        .keySchema(keys)
        .provisionedThroughput(throughput())
        .build());
      assertThat(result.tableDescription()).isNotNull();

    } catch (ResourceInUseException e) {
      // ignored for reruns against a shared integration container
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private ProvisionedThroughput throughput() {
    return ProvisionedThroughput.builder()
      .readCapacityUnits(1L)
      .writeCapacityUnits(1L)
      .build();
  }

  private <E> List<E> asList(E item) {
    List<E> list = new ArrayList<>();
    list.add(item);
    return list;
  }
}
