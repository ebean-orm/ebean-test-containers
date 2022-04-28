package io.ebean.docker.commands;

/**
 * MariaDB container.
 */
public class MariaDBContainer extends MySqlBaseContainer {

  public static class Builder extends DbConfig<MariaDBContainer, MariaDBContainer.Builder> {

    private Builder(String version) {
      super("mariadb", 4306, 3306, version);
      this.adminUsername = "root";
      this.adminPassword = "admin";
      this.setTmpfs("/var/lib/mysql:rw");
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
    public MariaDBContainer build() {
      return new MariaDBContainer(this);
    }
  }

  /**
   * Create a Builder for MySqlContainer.
   */
  public static Builder newBuilder(String version) {
    return new Builder(version);
  }

  private MariaDBContainer(Builder builder) {
    super(builder);
  }

}
