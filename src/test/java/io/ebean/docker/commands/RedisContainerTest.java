package io.ebean.docker.commands;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisContainerTest {

  @Test
  void checkConnectivity() {
    RedisConfig config = new RedisConfig("latest", new Properties());
    config.setPort(7379);
    config.setContainerName("temp_redis");

    RedisContainer container = new RedisContainer(config);
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
    properties.setProperty("redis.stopMode", "bar");

    RedisConfig config = new RedisConfig("latest", properties);

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
    properties.setProperty("ebean.test.redis.stopMode", "bar");

    RedisConfig config = new RedisConfig("latest", properties);

    assertProperties(config);
  }

  private void assertProperties(RedisConfig config) {
    assertEquals(config.getPort(), 7380);
    InternalConfig state = config.internalConfig();
    assertEquals(state.getInternalPort(), 5379);
    assertEquals(state.getImage(), "foo");
    assertEquals(state.getStartMode(), StartMode.Create);
    assertEquals(state.getStopMode(), StopMode.Stop);
  }
}
