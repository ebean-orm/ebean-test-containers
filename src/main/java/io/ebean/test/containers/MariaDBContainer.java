package io.ebean.test.containers;

/**
 * MariaDB container.
 */
public class MariaDBContainer extends BaseMySqlContainer<MariaDBContainer> {

  @Override
  public MariaDBContainer start() {
    startOrThrow();
    return this;
  }

  /**
   * Create a builder for MariaDBContainer.
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

  public static class Builder extends BaseDbBuilder<MariaDBContainer, Builder> {

    private Builder(String version) {
      super("mariadb", 4306, 3306, version);
      this.adminUsername = "root";
      this.adminPassword = "admin";
      this.tmpfs = "/var/lib/mysql:rw";
    }

    /**
     * Expose for MariaDB config.
     */
    protected Builder(String platform, int port, int internalPort, String version) {
      super(platform, port, internalPort, version);
    }


    @Override
    protected String buildJdbcUrl() {
      return "jdbc:mariadb://" + getHost() + ":" + getPort() + "/" + getDbName();
    }

    @Override
    protected String buildJdbcAdminUrl() {
      return "jdbc:mariadb://" + getHost() + ":" + getPort() + "/mysql";
    }

    @Override
    public MariaDBContainer build() {
      return new MariaDBContainer(this);
    }

    @Override
    public MariaDBContainer start() {
      return build().start();
    }
  }

  private MariaDBContainer(Builder builder) {
    super(builder);
  }

}
