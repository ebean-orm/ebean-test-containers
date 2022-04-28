package io.ebean.docker.commands;

import java.util.Properties;

public class MySqlConfig extends DbConfig {

  public MySqlConfig(String version, Properties properties) {
    this(version);
    setProperties(properties);
  }

  public MySqlConfig(String version) {
    super("mysql", 4306, 3306, version);
    this.adminUsername = "root";
    this.adminPassword = "admin";
    this.setTmpfs("/var/lib/mysql:rw");
  }

  /**
   * Expose for MariaDB config.
   */
  protected MySqlConfig(String platform, int port, int internalPort, String version) {
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
}
