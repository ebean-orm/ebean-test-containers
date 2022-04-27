package io.ebean.docker.commands;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class LocalDynamoContainerTest {

  @Test
  void start_via_LocalDynamoDB() {
    LocalDynamoDBContainer container = LocalDynamoDBContainer.newBuilder("1.13.2")
      //.port(8001)
      //.containerName("ut_dynamodb")
      //.image("amazon/dynamodb-local:1.13.2")
      .build();

    // start the container (if not already started)
    container.start();

    // obtain the AWS DynamoDB client
    AmazonDynamoDB amazonDynamoDB = container.dynamoDB();
    createTable(amazonDynamoDB);

    // container.stop();
    // container.stopRemove();
  }

  @Test
  void start() {
    LocalDynamoDBConfig config = new LocalDynamoDBConfig("1.13.2", null);
    //config.setPort(8001);
    LocalDynamoDBContainer container = new LocalDynamoDBContainer(config);
    container.start();

    AmazonDynamoDB dynamoDB = container.dynamoDB();
    createTable(dynamoDB);
  }

  private void createTable(AmazonDynamoDB dynamoDB) {
    try {
      List<KeySchemaElement> keys = asList(new KeySchemaElement("key", KeyType.HASH));
      List<AttributeDefinition> attrs = asList(new AttributeDefinition("key", ScalarAttributeType.S));
      CreateTableResult result = dynamoDB.createTable(new CreateTableRequest(attrs, "junk", keys, throughput()));
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
