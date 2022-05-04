package io.ebean.docker.commands;

public class MySqlContainer extends MySqlBaseContainer {

  /**
   * Create a builder for MySqlContainer.
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

  public static class Builder extends DbConfig<MySqlContainer, MySqlContainer.Builder> {

    private Builder(String version) {
      super("mysql", 4306, 3306, version);
      this.adminUsername = "root";
      this.adminPassword = "admin";
      this.tmpfs("/var/lib/mysql:rw");
    }

    /**
     * Expose for MariaDB config.
     */
    protected Builder(String platform, int port, int internalPort, String version) {
      super(platform, port, internalPort, version);
    }

    @Override
    protected String buildJdbcUrl() {
      return "jdbc:mysql://" + getHost() + ":" + getPort() + "/" + getDbName();
    }

    @Override
    protected String buildJdbcAdminUrl() {
      return "jdbc:mysql://" + getHost() + ":" + getPort() + "/mysql";
    }

    @Override
    public MySqlContainer build() {
      return new MySqlContainer(this);
    }
  }

  private MySqlContainer(Builder builder) {
    super(builder);
  }

}
