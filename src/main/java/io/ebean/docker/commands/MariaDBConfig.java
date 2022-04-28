package io.ebean.docker.commands;

import java.util.Properties;

/**
 * MariaDB configuration. Uses mariadb image.
 */
public class MariaDBConfig extends MySqlConfig {

  public MariaDBConfig(String version, Properties properties) {
    this(version);
    setProperties(properties);
  }

  public MariaDBConfig(String version) {
    super("mariadb", 4306, 3306, version);
    this.adminUsername = "root";
    this.adminPassword = "admin";
    this.setTmpfs("/var/lib/mysql:rw");
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
