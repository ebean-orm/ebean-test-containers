package io.ebean.docker.commands;

import java.util.Properties;

public class RedisConfig extends BaseConfig {

  public RedisConfig(String version, Properties properties) {
    super("redis", "6379", "6379", version);
    this.image = "redis:" + version;
    this.containerName = "ut_redis";
    setProperties(properties);
  }
}
