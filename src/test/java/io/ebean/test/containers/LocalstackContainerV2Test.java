package io.ebean.test.containers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.CreateStreamRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalstackContainerV2Test {

  @Disabled
  @Test
  void start_viaBuilder() {
    LocalstackContainer container = LocalstackContainer.builder("0.14.4")
      .awsRegion("ap-southeast-2")
      .services("dynamodb,kinesis,sns,sqs")
      //.port(4567)
      .containerName("ut_localstack_dkss")
      .image("localstack/localstack:0.14.4")
      .build();

    // container.stopRemove();
    container.startMaybe();

    var amazonDynamoDB = container.dynamoDBClient();
    createTable(amazonDynamoDB);

    KinesisClient kinesis = container.kinesisClient();
    SnsClient sns = container.snsClient();
    SqsClient sqs = container.sqsClient();

    useSnsSqs(container);
    useKinesis(container);

    assertThat(container.credentials()).isNotNull();
    assertThat(container.endpointUrl()).isNotNull();
    assertThat(container.awsRegion()).isEqualTo("ap-southeast-2");

    assertThat(kinesis).isNotNull();
    assertThat(sns).isNotNull();
    assertThat(sqs).isNotNull();

    // container.stopRemove();
  }

  private void useKinesis(LocalstackContainer container) {
    KinesisClient kinesis = container.kinesisClient();
    try {
      System.out.println(kinesis.createStream(CreateStreamRequest.builder()
        .streamName("hello-stream")
        .shardCount(1)
        .build()));
    } catch (com.amazonaws.services.kinesis.model.ResourceInUseException e) {
      System.out.println("stream already exists");
    }
  }

  private void useSnsSqs(LocalstackContainer container) {

    SqsClient sqs = container.sqsClient();
    SnsClient sns = container.snsClient();

    String sqsName = "SQS_NAME";
    String snsTopicName = "SNS_TOPIC";
    String sqsUrl = "http://localhost:4566/000000000000/SQS_NAME";
    String snsTopicArn = "arn:aws:sns:ap-southeast-2:000000000000:SNS_TOPIC";
    String sqsArn = "arn:aws:sqs:ap-southeast-2:000000000000:SQS_NAME";
    try {
      sqsUrl = sqs.createQueue(CreateQueueRequest.builder().queueName(sqsName).build()).queueUrl();
      snsTopicArn = sns.createTopic(CreateTopicRequest.builder().name(snsTopicName).build()).topicArn();
      sqsArn = sqs.getQueueAttributes(GetQueueAttributesRequest.builder()
          .queueUrl(sqsUrl).attributeNames(QueueAttributeName.QUEUE_ARN)
          .build())
        .attributes().get(QueueAttributeName.QUEUE_ARN);

//      Policy allowSnsToPostToSqsPolicy = new Policy("allow sns " + snsTopicArn + " to send to queue", singletonList(new Statement(Statement.Effect.Allow)));
//      sqs.setQueueAttributes(SetQueueAttributesRequest.builder()
//        .queueUrl(sqsUrl)
//          .attributes(Map.of(QueueAttributeName.POLICY, allowSnsToPostToSqsPolicy.toJson()));

    } catch (Exception e) {
      System.out.println("queue exists");
      e.printStackTrace();
    }

    final String topic = snsTopicArn;

    String sqsSubscriptionArn = sns.subscribe(SubscribeRequest.builder()
        .topicArn(snsTopicArn)
        .protocol("sqs")
        .endpoint(sqsArn)
        .build())
      .subscriptionArn();

    sns.publish(PublishRequest.builder().topicArn(topic).message("Hello 0").build());
    sns.publish((b) -> b.topicArn(topic).message("Hello 1"));
    sns.publish((b) -> b.topicArn(topic).message("Hello 2"));

    ReceiveMessageResponse receiveResp = sqs.receiveMessage(ReceiveMessageRequest.builder()
      .queueUrl(sqsUrl)
      .maxNumberOfMessages(2)
      .waitTimeSeconds(10)
      .build());
    for (Message message : receiveResp.messages()) {
      System.out.println("got message" + message);
    }

    sns.deleteTopic(b -> b.topicArn(topic));

    final String queueUrl = sqsUrl;
    sqs.deleteQueue(b -> b.queueUrl(queueUrl));
  }

  @Disabled
  @Test
  void randomPort() {
    LocalstackContainer container = LocalstackContainer.builder("0.14")
      .port(0)
      .build();

    container.startMaybe();

    int assignedPort = container.port();
    assertThat(assignedPort).isGreaterThan(0);

    DynamoDbClient amazonDynamoDB = container.dynamoDBClient();
    createTable(amazonDynamoDB);

    container.stop();
    container.stop();
  }

  @Disabled
  @Test
  void start() {

    LocalstackContainer container = LocalstackContainer.builder("0.14.4")
      //.setShutdownMode(StopMode.None)
      .build();
    container.startMaybe();

    DynamoDbClient amazonDynamoDB = container.dynamoDBClient();
    createTable(amazonDynamoDB);

    //container.stop();
  }

  private void createTable(DynamoDbClient dynamoDB) {
    try {
      List<KeySchemaElement> keys = asList(KeySchemaElement.builder()
        .attributeName("key").keyType(KeyType.HASH).build());
      List<AttributeDefinition> attrs = asList(AttributeDefinition.builder()
        .attributeName("key").attributeType(ScalarAttributeType.S).build());

      CreateTableResponse result = dynamoDB.createTable(CreateTableRequest.builder()
        .attributeDefinitions(attrs)
        .tableName("junk-localstack2")
        .keySchema(keys)
        .provisionedThroughput(throughput())
        .build());
      System.out.println(result);

    } catch (ResourceInUseException e) {
      System.out.println("table already exists !!");
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

  <E> List<E> asList(E item) {
    List<E> list = new ArrayList<>();
    list.add(item);
    return list;
  }
}
