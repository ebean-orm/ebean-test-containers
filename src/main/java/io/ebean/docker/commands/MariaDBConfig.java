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
    super("mariadb", "4306", "3306", version);
    this.setTmpfs("/var/lib/mysql:rw");
  }

  public String jdbcUrl() {
    return "jdbc:mysql://localhost:" + getPort() + "/" + getDbName();
  }
}
