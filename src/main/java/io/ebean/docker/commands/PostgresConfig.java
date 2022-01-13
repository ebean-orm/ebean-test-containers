package io.ebean.docker.commands;

import java.util.Properties;

public class PostgresConfig extends DbConfig {

  public PostgresConfig(String version, Properties properties) {
    this(version);
    setProperties(properties);
  }

  public PostgresConfig(String version) {
    super("postgres", 6432, 5432, version);
    this.adminUsername = "postgres";
    setTmpfs("/var/lib/postgresql/data:rw");
  }

  @Override
  public String jdbcUrl() {
    return "jdbc:postgresql://" + getHost() + ":" + getPort() + "/" + getDbName();
  }

  @Override
  public String jdbcAdminUrl() {
    return "jdbc:postgresql://" + getHost() + ":" + getPort() + "/postgres";
  }
}
