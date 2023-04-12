package io.ebean.test.containers;

/**
 * Commands for controlling a postgis docker container.
 */
public class PostgisContainer extends BasePostgresContainer implements Container {

  /**
   * Create a builder for PostgisContainer.
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

  private PostgisContainer(Builder config) {
    super(config);
  }

  /**
   * Builder for Postgis container.
   */
  public static class Builder extends DbConfig<PostgisContainer, Builder> {

    private Builder(String version) {
      super("postgis", 6432, 5432, version);
      this.image = "ghcr.io/baosystems/postgis:" + version;
      this.adminUsername = "postgres";
      this.tmpfs = "/var/lib/postgresql/data:rw";
      this.extensions = "hstore,pgcrypto,postgis";
      this.extraDbExtensions = extensions;
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
    public PostgisContainer build() {
      return new PostgisContainer(this);
    }
  }
}
