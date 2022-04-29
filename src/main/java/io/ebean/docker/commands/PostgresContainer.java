package io.ebean.docker.commands;

import io.ebean.docker.container.Container;
import io.ebean.docker.container.StopMode;

/**
 * Commands for controlling a postgres docker container.
 */
public class PostgresContainer extends BasePostgresContainer implements Container {

  /**
   * Create a builder for the PostgresContainer.
   */
  public static Builder newBuilder(String version) {
    return new Builder(version);
  }

  private PostgresContainer(Builder config) {
    super(config);
  }

  /**
   * Builder for Postgres container.
   */
  public static class Builder extends DbConfig<PostgresContainer, Builder> {

    private Builder(String version) {
      super("postgres", 6432, 5432, version);
      this.adminUsername = "postgres";
      stopMode(StopMode.Remove);
      tmpfs("/var/lib/postgresql/data:rw");
    }

    @Override
    protected String buildJdbcUrl() {
      return "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
    }

    @Override
    protected String buildJdbcAdminUrl() {
      return "jdbc:postgresql://" + host + ":" + port + "/postgres";
    }

    @Override
    public PostgresContainer build() {
      return new PostgresContainer(this);
    }
  }
}
