package io.ebean.docker.commands;

import java.util.Properties;

public class ClickHouseConfig extends DbConfig {

  public ClickHouseConfig(String version, Properties properties) {
    this(version);
    setProperties(properties);
  }

  public ClickHouseConfig(String version) {
    super("clickhouse", 8123, 8123, version);
    this.image = "yandex/clickhouse-server:" + version;
    this.setUser("default");
    this.setPassword("");
    this.adminUsername = "default";
    this.adminPassword = "";
  }

  @Override
  public String jdbcUrl() {
    return "jdbc:clickhouse://localhost:" + getPort() + "/" + getDbName();
  }

  @Override
  public String jdbcAdminUrl() {
    return "jdbc:clickhouse://localhost:" + getPort()+ "/default";
  }
}
