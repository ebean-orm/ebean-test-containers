package io.ebean.test.containers;

/**
 * Commands for controlling a postgres docker container.
 */
public class PostgresContainer extends BasePostgresContainer<PostgresContainer> {

  @Override
  public PostgresContainer start() {
    startOrThrow();
    return this;
  }

  /**
   * Create a builder for PostgresContainer.
   */
  public static Builder builder(String version) {
    return new Builder(version);
  }

  /**
   * Deprecated - migrate to builder().
   */
  @Deprecated
  public static Builder newBuilder(String version) {
    return builder(version);
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
      this.tmpfs = "/var/lib/postgresql/data:rw";
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
    protected String buildExtraJdbcUrl() {
      return "jdbc:postgresql://" + host + ":" + port + "/" + extraDb;
    }

    @Override
    public PostgresContainer build() {
      return new PostgresContainer(this);
    }

    @Override
    public PostgresContainer start() {
      return build().start();
    }
  }
}
