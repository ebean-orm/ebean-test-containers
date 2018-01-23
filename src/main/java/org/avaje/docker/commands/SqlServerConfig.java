package org.avaje.docker.commands;

import java.util.Properties;

/**
 * Sql Server configuration.
 */
public class SqlServerConfig extends DbConfig {

  public SqlServerConfig(String version, Properties properties) {
    this(version);
    withProperties(properties);
  }

  public SqlServerConfig(String version) {
    super("sqlserver", "1433", "1433", version);
    this.image = "microsoft/mssql-server-linux:" + version;
    // default password that satisfies sql server
    withAdminPassword("SqlS3rv#r");
    withPassword("SqlS3rv#r");
  }

  public String jdbcUrl() {
    return "jdbc:sqlserver://localhost:" + getPort() + ";databaseName=" + getDbName();
  }
}
