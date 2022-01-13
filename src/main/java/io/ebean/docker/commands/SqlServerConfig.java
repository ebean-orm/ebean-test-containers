package io.ebean.docker.commands;

import java.util.Properties;

/**
 * Sql Server configuration.
 */
public class SqlServerConfig extends DbConfig {

  public SqlServerConfig(String version, Properties properties) {
    this(version);
    setProperties(properties);
  }

  public SqlServerConfig(String version) {
    super("sqlserver", 1433, 1433, version);
    this.image = "mcr.microsoft.com/mssql/server:" + version;
    // default password that satisfies sql server
    this.adminUsername = "sa";
    this.adminPassword = "SqlS3rv#r";
    this.password = "SqlS3rv#r";
  }

  public String jdbcUrl() {
    return "jdbc:sqlserver://" + getHost() + ":" + getPort() + ";databaseName=" + getDbName();
  }

  @Override
  public String jdbcAdminUrl() {
    return "jdbc:sqlserver://" + getHost() + ":" + getPort();
  }
}
