package io.ebean.docker.commands;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalstackContainerTest {

  @Test
  void start_viaBuilder() {
    LocalstackContainer container = LocalstackContainer.newBuilder("0.14")
      .services("dynamodb,kinesis,sns")
      // .port(4566)
      // .image("localstack/localstack:0.14")
      .build();

    container.start();

    AmazonDynamoDB amazonDynamoDB = container.dynamoDB();
    createTable(amazonDynamoDB);

    AmazonKinesis kinesis = container.kinesis();
    AmazonSNS sns = container.sns();

    useSns(container);
    useKinesis(container);

    assertThat(container.credentials()).isNotNull();
    assertThat(container.endpointUrl()).isNotNull();
    assertThat(container.awsRegion()).isEqualTo("ap-southeast-2");

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

  private void useSns(LocalstackContainer container) {
    AmazonSNS sns = container.sns();
    System.out.println(sns.listTopics());
    CreateTopicResult topic = sns.createTopic(new CreateTopicRequest("hello-topic"));
    System.out.println(topic);
    String topicArn = topic.getTopicArn();
    sns.publish(topicArn, "one");
    sns.publish(topicArn, "two");
  }

  @Test
  void start() {
    LocalstackConfig config = new LocalstackConfig("0.14", null);

    LocalstackContainer container = new LocalstackContainer(config);
    container.start();

    AmazonDynamoDB amazonDynamoDB = container.dynamoDB();
    createTable(amazonDynamoDB);

    //container.stopRemove();
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
