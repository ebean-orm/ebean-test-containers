package io.ebean.docker.commands;

import java.util.Properties;

public class PostgresConfig extends DbConfig {

  public PostgresConfig(String version, Properties properties) {
    this(version);
    setProperties(properties);
  }

  public PostgresConfig(String version) {
    super("postgres", "6432", "5432", version);
    setTmpfs("/var/lib/postgresql/data:rw");
  }

  public String jdbcUrl() {
    return "jdbc:postgresql://localhost:" + getPort() + "/" + getDbName();
  }
}
