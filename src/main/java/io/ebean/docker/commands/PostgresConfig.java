package io.ebean.docker.commands;

import io.ebean.docker.container.StopMode;

import java.util.Properties;

public class PostgresConfig extends PostgresContainer.Builder {

  public PostgresConfig(String version, Properties properties) {
    this(version);
    properties(properties);
  }

  public PostgresConfig(String version) {
    super(version);
    stopMode(StopMode.Remove);
  }
}
