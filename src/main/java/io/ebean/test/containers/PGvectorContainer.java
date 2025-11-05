package io.ebean.test.containers;

/**
 * Commands for controlling a pgvector docker container.
 */
public class PGvectorContainer extends BasePostgresContainer<PGvectorContainer> {

  @Override
  public PGvectorContainer start() {
    startOrThrow();
    return this;
  }

  /**
   * Create a builder for PGvectorContainer.
   */
  public static Builder builder(String version) {
    return new Builder(version);
  }

  private PGvectorContainer(Builder config) {
    super(config);
  }

  /**
   * Builder for Postgis container.
   */
  public static class Builder extends BaseDbBuilder<PGvectorContainer, Builder> {

    private Builder(String version) {
      super("pgvector", 6435, 5432, version);
      this.image = "pgvector/pgvector:" + version;
      this.adminUsername = "postgres";
      this.tmpfs = "/var/lib/postgresql/data:rw";
      this.extensions = "vector";
      this.extra.extensions = extensions;
      this.extra2.extensions = extensions;
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
    protected String buildExtraJdbcUrl(String dbName) {
      return "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
    }

    @Override
    public PGvectorContainer build() {
      return new PGvectorContainer(this);
    }

    @Override
    public PGvectorContainer start() {
      return build().start();
    }
  }
}
