package io.ebean.test.containers;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
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

    assertThat(container.connectionString()).isEqualTo("mongodb://admin:secret@localhost:27017/mydb");
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
    properties.setProperty("mongodb.image", "mongo:7.0");
    properties.setProperty("mongodb.port", "27018");
    properties.setProperty("mongodb.containerName", "mongo_junk8");
    properties.setProperty("mongodb.internalPort", "27017");
    properties.setProperty("mongodb.username", "admin");
    properties.setProperty("mongodb.password", "secret");
    properties.setProperty("mongodb.dbName", "mydb");
    properties.setProperty("mongodb.shutdownMode", "stop");

    InternalConfig config = MongoContainer.builder("8.0")
      .properties(properties)
      .internalConfig();
    config.setDefaultContainerName();

    assertProperties(config);
  }

  @Test
  void properties_with_ebeanTestPrefix() {
    Properties properties = new Properties();
    properties.setProperty("ebean.test.mongodb.image", "mongo:7.0");
    properties.setProperty("ebean.test.mongodb.port", "27018");
    properties.setProperty("ebean.test.mongodb.containerName", "mongo_junk8");
    properties.setProperty("ebean.test.mongodb.internalPort", "27017");
    properties.setProperty("ebean.test.mongodb.username", "admin");
    properties.setProperty("ebean.test.mongodb.password", "secret");
    properties.setProperty("ebean.test.mongodb.dbName", "mydb");
    properties.setProperty("ebean.test.mongodb.shutdownMode", "stop");

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
