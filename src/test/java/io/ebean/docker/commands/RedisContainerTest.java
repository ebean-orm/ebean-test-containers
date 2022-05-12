package io.ebean.docker.commands;

import io.ebean.docker.container.StartMode;
import io.ebean.docker.container.StopMode;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisContainerTest {

  @Test
  void randomPort() {
    RedisContainer container = RedisContainer.builder("latest")
      .port(0)
      .build();

    assertTrue(container.start());
    assertThat(container.port()).isGreaterThan(0);
  }

  @Test
  void checkConnectivity() {
    RedisContainer container = RedisContainer.builder("latest")
      .port(7379)
      .containerName("temp_redis")
      .build();

    assertTrue(container.start());
    container.stopRemove();
  }

  @Test
  void properties_with_noPrefix() {
    Properties properties = new Properties();
    properties.setProperty("redis.image", "foo");
    properties.setProperty("redis.port", "7380");
    properties.setProperty("redis.containerName", "redis_junk8");
    properties.setProperty("redis.internalPort", "5379");
    properties.setProperty("redis.startMode", "baz");
    properties.setProperty("redis.shutdownMode", "bar");

    InternalConfig config = RedisContainer.builder("latest")
      .properties(properties)
      .internalConfig();
    assertProperties(config);
  }

  @Test
  void properties_with_ebeanTestPrefix() {
    Properties properties = new Properties();
    properties.setProperty("ebean.test.redis.image", "foo");
    properties.setProperty("ebean.test.redis.port", "7380");
    properties.setProperty("ebean.test.redis.containerName", "redis_junk8");
    properties.setProperty("ebean.test.redis.internalPort", "5379");
    properties.setProperty("ebean.test.redis.startMode", "baz");
    properties.setProperty("ebean.test.redis.shutdownMode", "bar");

    InternalConfig config = RedisContainer.builder("latest")
      .properties(properties)
      .internalConfig();

    config.setDefaultContainerName();
    assertProperties(config);
  }

  private void assertProperties(InternalConfig config) {
    assertEquals(config.getPort(), 7380);
    assertEquals(config.getInternalPort(), 5379);
    assertEquals(config.getImage(), "foo");
    assertEquals(config.getStartMode(), StartMode.Create);
    assertEquals(config.shutdownMode(), StopMode.Stop);
    assertEquals(config.containerName(), "redis_junk8");
  }
}
