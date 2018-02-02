package org.avaje.docker.commands;

import java.util.Properties;

public class MySqlConfig extends DbConfig {

  public MySqlConfig(String version, Properties properties) {
    this(version);
    setProperties(properties);
  }

  public MySqlConfig(String version) {
    super("mysql", "4306", "3306", version);
    this.setTmpfs("/var/lib/mysql:rw");
  }

  public String jdbcUrl() {
    return "jdbc:mysql://localhost:" + getPort() + "/" + getDbName();
  }
}
