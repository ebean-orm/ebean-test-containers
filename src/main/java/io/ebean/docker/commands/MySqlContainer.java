package io.ebean.docker.commands;

import io.ebean.docker.container.CBuilder;

public class MySqlContainer extends MySqlBaseContainer {

  public static class Builder extends DbConfig<MySqlContainer.Builder> implements CBuilder<MySqlContainer, MySqlContainer.Builder> {

    private Builder(String version) {
      super("mysql", 4306, 3306, version);
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
    public MySqlContainer build() {
      return new MySqlContainer(this);
    }
  }

  /**
   * Create a Builder for MySqlContainer.
   */
  public static Builder newBuilder(String version) {
    return new Builder(version);
  }

  private MySqlContainer(Builder builder) {
    super(builder);
  }

}
