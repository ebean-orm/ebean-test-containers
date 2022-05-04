package io.ebean.docker.commands;

import io.ebean.docker.container.Container;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Commands for controlling a SqlServer docker container.
 */
public class SqlServerContainer extends JdbcBaseDbContainer implements Container {

  /**
   * Create a builder for SqlServerContainer.
   */
  public static Builder builder(String version) {
    return new Builder(version);
  }

  /**
   * Deprecated - migrate to builder().
   */
  @Deprecated
  public static Builder newBuilder(String version) {
    return builder(version);
  }

  /**
   * Builder for SqlServerContainer.
   */
  public static class Builder extends DbConfig<SqlServerContainer, SqlServerContainer.Builder> {

    private Builder(String version) {
      super("sqlserver", 1433, 1433, version);
      this.image = "mcr.microsoft.com/mssql/server:" + version;
      // default password that satisfies sql server
      this.adminUsername = "sa";
      this.adminPassword = "SqlS3rv#r";
      this.password = "SqlS3rv#r";
    }

    @Override
    protected String buildJdbcUrl() {
      return "jdbc:sqlserver://" + getHost() + ":" + getPort() + ";databaseName=" + getDbName() + ";integratedSecurity=false;trustServerCertificate=true";
    }

    @Override
    protected String buildJdbcAdminUrl() {
      return "jdbc:sqlserver://" + getHost() + ":" + getPort() + ";integratedSecurity=false;trustServerCertificate=true";
    }

    @Override
    public SqlServerContainer build() {
      return new SqlServerContainer(this);
    }
  }

  private SqlServerContainer(Builder builder) {
    super(builder);
  }

  @Override
  void createDatabase() {
    createRoleAndDatabase(false);
  }

  @Override
  void dropCreateDatabase() {
    createRoleAndDatabase(true);
  }

  private void createRoleAndDatabase(boolean withDrop) {
    try (Connection connection = config.createAdminConnection()) {
      if (withDrop) {
        dropDatabaseIfExists(connection);
      }
      createDatabase(connection);
      createLogin(connection);
      createUser();

    } catch (SQLException e) {
      throw new RuntimeException("Error when creating database and role", e);
    }
  }

  private void createUser() {
    try (Connection dbConnection = dbConfig.createAdminConnection(dbConfig.jdbcUrl())) {
      createUser(dbConnection);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private void createLogin(Connection connection) {
    if (!loginExists(connection, dbConfig.getUsername())) {
      createLogin(connection, dbConfig.getUsername(), dbConfig.getPassword());
    }
  }

  private void createUser(Connection dbConnection) {
    if (!userExists(dbConnection, dbConfig.getUsername())) {
      createUser(dbConnection, dbConfig.getUsername(), dbConfig.getUsername());
      grantOwner(dbConnection, dbConfig.getUsername());
    }
  }

  private void createDatabase(Connection connection) {
    if (!databaseExists(connection, dbConfig.getDbName())) {
      createDatabase(connection, dbConfig.getDbName());
    }
  }

  private void dropDatabaseIfExists(Connection connection) {
    if (databaseExists(connection, dbConfig.getDbName())) {
      dropDatabase(connection, dbConfig.getDbName());
    }
  }

  private void dropDatabase(Connection connection, String dbName) {
    sqlRun(connection, "drop database " + dbName);
  }

  private void createDatabase(Connection connection, String dbName) {
    sqlRun(connection, "create database " + dbName);
  }

  private void createLogin(Connection connection, String login, String pass) {
    sqlRun(connection, "create login " + login + " with password = '" + pass + "'");
  }

  private void createUser(Connection dbConnection, String roleName, String login) {
    sqlRun(dbConnection, "create user " + roleName + " for login " + login);
  }

  private void grantOwner(Connection dbConnection, String roleName) {
    sqlRun(dbConnection, "exec sp_addrolemember 'db_owner', " + roleName);
  }

  private boolean userExists(Connection dbConnection, String userName) {
    return sqlHasRow(dbConnection, "select 1 from sys.database_principals where name = '" + userName + "'");
  }

  private boolean loginExists(Connection connection, String roleName) {
    return sqlHasRow(connection, "select 1 from master.dbo.syslogins where loginname = '" + roleName + "'");
  }

  private boolean databaseExists(Connection connection, String dbName) {
    Exception lastError = null;
    for (int i = 0; i < 3; i++) {
      try {
        return databaseExistsAttempt(connection, dbName);
      } catch (Exception e) {
        log.info("Failed databaseExistsAttempt() {} with {}", i, e.getMessage());
        lastError = e;
        waitTime();
      }
    }
    log.warn("Failed databaseExists() check with last error captured of ...", lastError);
    throw new IllegalStateException("Failed databaseExists check with", lastError);
  }

  private void waitTime() {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted during databaseExists");
    }
  }

  private boolean databaseExistsAttempt(Connection connection, String dbName) {
    return sqlHasRow(connection, "select 1 from sys.databases where name='" + dbName + "'");
  }

  @Override
  protected ProcessBuilder runProcess() {

    List<String> args = dockerRun();
    args.add("-e");
    args.add("ACCEPT_EULA=Y");
    args.add("-e");
    args.add("SA_PASSWORD=" + dbConfig.getAdminPassword());

    if (dbConfig.isDefaultCollation()) {
      // do nothing, use server default
    } else if (dbConfig.isExplicitCollation()) {
      args.add("-e");
      args.add("MSSQL_COLLATION=" + dbConfig.getCollation());
    } else {
      // use case sensitive collation by default
      args.add("-e");
      args.add("MSSQL_COLLATION=Latin1_General_100_BIN2");
    }
    args.add(config.getImage());
    return createProcessBuilder(args);
  }

}
