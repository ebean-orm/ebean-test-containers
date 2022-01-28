package io.ebean.docker.commands;

import io.ebean.docker.container.Container;

import java.util.Properties;

/**
 * Commands for controlling a postgres docker container.
 * <p>
 * References: https://github.com/docker-library/postgres/issues/146
 */
public class PostgresContainer extends BasePostgresContainer implements Container {

  /**
   * Create Postgres container with configuration from properties.
   */
  public static PostgresContainer create(String pgVersion, Properties properties) {
    return new PostgresContainer(new PostgresConfig(pgVersion, properties));
  }

  public PostgresContainer(PostgresConfig config) {
    super(config);
  }

}
