package io.ebean.docker.commands;

import java.util.Properties;

public class CockroachConfig extends DbConfig {

  public CockroachConfig(String version, Properties properties) {
    this(version);
    setProperties(properties);
  }

  public CockroachConfig(String version) {
    super("cockroach", 26257, 26257, version);
    this.image = "cockroachdb/cockroach:" + version;
    this.adminInternalPort = 8080;
    this.adminPort = 8888;
    this.setUser("root");
  }

  public CockroachConfig() {
    this("v21.2.4");
  }

  public String jdbcUrl() {
    return "jdbc:postgresql://" + getHost() + ":" + getPort() + "/" + getDbName() + "?sslmode=disable";
  }
}
