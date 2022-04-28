package io.ebean.docker.commands;

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class LocalstackContainerTest {

  @Test
  void start_viaBuilder() {
    LocalstackContainer container = LocalstackContainer.newBuilder("0.14")
      .awsRegion("ap-southeast-2")
      .services("dynamodb,kinesis,sns,sqs")
      .port(4566)
      .image("localstack/localstack:0.14")
      .build();

    // container.stopRemove();
    container.start();

    AmazonDynamoDB amazonDynamoDB = container.dynamoDB();
    createTable(amazonDynamoDB);

    AmazonKinesis kinesis = container.kinesis();
    AmazonSNS sns = container.sns();
    AmazonSQS sqs = container.sqs();

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
    AmazonKinesis kinesis = container.kinesis();
    try {
      System.out.println(kinesis.createStream("hello-stream", 1));
    } catch (com.amazonaws.services.kinesis.model.ResourceInUseException e) {
      System.out.println("stream already exists");
    }
  }

  private void useSnsSqs(LocalstackContainer container) {

    AmazonSQS sqs = container.sqs();
    AmazonSNS sns = container.sns();

    String sqsName = "SQS_NAME";
    String snsTopicName = "SNS_TOPIC";

    String sqsUrl = sqs.createQueue(new CreateQueueRequest(sqsName)).getQueueUrl();
    String snsTopicArn = sns.createTopic(snsTopicName).getTopicArn();
    String sqsArn = sqs.getQueueAttributes(sqsUrl, singletonList("QueueArn")).getAttributes().get("QueueArn");

    Policy allowSnsToPostToSqsPolicy = new Policy("allow sns " + snsTopicArn + " to send to queue", singletonList(new Statement(Statement.Effect.Allow)));
    sqs.setQueueAttributes(new SetQueueAttributesRequest().withQueueUrl(sqsUrl).addAttributesEntry("Policy", allowSnsToPostToSqsPolicy.toJson()));
    String sqsSubscriptionArn = sns.subscribe(snsTopicArn, "sqs", sqsArn).getSubscriptionArn();
    sns.publish(snsTopicArn, "Hello 0");
    sns.publish(snsTopicArn, "Hello 1");
    sns.publish(snsTopicArn, "Hello 2");

    ReceiveMessageResult receiveResp = sqs.receiveMessage(new ReceiveMessageRequest(sqsUrl).withMaxNumberOfMessages(2).withWaitTimeSeconds(10));
    for (Message message : receiveResp.getMessages()) {
      System.out.println("got message" + message);
    }

    sns.deleteTopic(snsTopicArn);
    sqs.deleteQueue(sqsUrl);
  }

  @Test
  void start() {

    LocalstackContainer container = LocalstackContainer.newBuilder("0.14")
      //.setShutdownMode(StopMode.None)
      .build();
    container.start();

    AmazonDynamoDB amazonDynamoDB = container.dynamoDB();
    createTable(amazonDynamoDB);

    container.stop();
  }

  private void createTable(AmazonDynamoDB dynamoDB) {
    try {
      List<KeySchemaElement> keys = asList(new KeySchemaElement("key", KeyType.HASH));
      List<AttributeDefinition> attrs = asList(new AttributeDefinition("key", ScalarAttributeType.S));
      CreateTableResult result = dynamoDB.createTable(new CreateTableRequest(attrs, "junk-localstack", keys, throughput()));
      System.out.println(result);

    } catch (ResourceInUseException e) {
      System.out.println("table already exists !!");
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private ProvisionedThroughput throughput() {
    return new ProvisionedThroughput(1L, 1L);
  }

  <E> List<E> asList(E item) {
    List<E> list = new ArrayList<>();
    list.add(item);
    return list;
  }
}
