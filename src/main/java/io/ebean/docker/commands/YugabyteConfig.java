package io.ebean.docker.commands;

import java.util.Properties;

/**
 * Yugabyte DB configuration.
 */
public class YugabyteConfig extends DbConfig {

  public YugabyteConfig(String version, Properties properties) {
    this(version);
    setProperties(properties);
  }

  public YugabyteConfig(String version) {
    super("yugabyte", 6433, 5433, version);
    this.image = "yugabytedb/yugabyte:" + version;
    this.adminUsername = "postgres";
    //setTmpfs("/var/lib/postgresql/data:rw");
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