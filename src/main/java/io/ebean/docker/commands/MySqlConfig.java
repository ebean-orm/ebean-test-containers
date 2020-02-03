package io.ebean.docker.commands;

import java.util.Properties;

public class MySqlConfig extends DbConfig {

  public MySqlConfig(String version, Properties properties) {
    this(version);
    setProperties(properties);
  }

  public MySqlConfig(String version) {
    super("mysql", "4306", "3306", version);
    this.adminUsername = "root";
    this.adminPassword = "admin";
    this.setTmpfs("/var/lib/mysql:rw");
  }

  /**
   * Expose for MariaDB config.
   */
  protected MySqlConfig(String platform, String port, String internalPort, String version) {
    super(platform, port, internalPort, version);
  }

  @Override
  public String jdbcUrl() {
    return "jdbc:mysql://localhost:" + getPort() + "/" + getDbName();
  }

  @Override
  public String jdbcAdminUrl() {
    return "jdbc:mysql://localhost:" + getPort() + "/mysql";
  }
}
