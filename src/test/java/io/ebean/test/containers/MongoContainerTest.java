package io.ebean.test.containers;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MongoContainerTest {

  @Test
  void randomPort() {
    MongoContainer container = MongoContainer.builder("8.0")
      .port(0)
      .build();

    assertTrue(container.startMaybe());
    assertThat(container.port()).isGreaterThan(0);
    assertThat(container.connectionString()).contains(":" + container.port() + "/");
  }

  @Test
  void checkConnectivity() {
    MongoContainer container = MongoContainer.builder("8.0")
      .port(27017)
      .containerName("temp_mongo")
      .build();

    assertTrue(container.startMaybe());

    try (MongoClient client = container.mongoClient()) {
      MongoDatabase db = client.getDatabase(container.dbName());
      assertThat(db.getName()).isEqualTo("test");
      Document result = db.runCommand(new Document("ping", 1));
      assertThat(result.getDouble("ok")).isEqualTo(1.0);
    }

    container.stopRemove();
  }

  @Test
  void connectionString_withAuth() {
    MongoContainer container = MongoContainer.builder("8.0")
      .username("admin")
      .password("secret")
      .dbName("mydb")
      .build();

    assertThat(container.connectionString()).isEqualTo("mongodb://admin:secret@localhost:27017/mydb?authSource=admin");
  }

  @Test
  void connectionString_noAuth() {
    MongoContainer container = MongoContainer.builder("8.0")
      .username("")
      .password("")
      .dbName("mydb")
      .build();

    assertThat(container.connectionString()).isEqualTo("mongodb://localhost:27017/mydb");
  }

  @Test
  void properties_with_noPrefix() {
    Properties properties = new Properties();
    properties.setProperty("mongo.image", "mongo:7.0");
    properties.setProperty("mongo.port", "27018");
    properties.setProperty("mongo.containerName", "mongo_junk8");
    properties.setProperty("mongo.internalPort", "27017");
    properties.setProperty("mongo.username", "admin");
    properties.setProperty("mongo.password", "secret");
    properties.setProperty("mongo.dbName", "mydb");
    properties.setProperty("mongo.shutdownMode", "stop");

    InternalConfig config = MongoContainer.builder("8.0")
      .properties(properties)
      .internalConfig();
    config.setDefaultContainerName();

    assertProperties(config);
  }

  @Test
  void properties_with_ebeanTestPrefix() {
    Properties properties = new Properties();
    properties.setProperty("ebean.test.mongo.image", "mongo:7.0");
    properties.setProperty("ebean.test.mongo.port", "27018");
    properties.setProperty("ebean.test.mongo.containerName", "mongo_junk8");
    properties.setProperty("ebean.test.mongo.internalPort", "27017");
    properties.setProperty("ebean.test.mongo.username", "admin");
    properties.setProperty("ebean.test.mongo.password", "secret");
    properties.setProperty("ebean.test.mongo.dbName", "mydb");
    properties.setProperty("ebean.test.mongo.shutdownMode", "stop");

    InternalConfig config = MongoContainer.builder("8.0")
      .properties(properties)
      .internalConfig();
    config.setDefaultContainerName();

    assertProperties(config);
  }

  private void assertProperties(InternalConfig config) {
    assertEquals(27018, config.getPort());
    assertEquals(27017, config.getInternalPort());
    assertEquals("mongo:7.0", config.getImage());
    assertEquals(StopMode.Stop, config.shutdownMode());
    assertEquals("mongo_junk8", config.containerName());
  }
}
