package io.ebean.docker.commands;

import io.ebean.docker.container.Container;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

public class MySqlContainer extends JdbcBaseDbContainer implements Container {

  public static MySqlContainer create(String mysqlVersion, Properties properties) {
    return new MySqlContainer(new MySqlConfig(mysqlVersion, properties));
  }

  public MySqlContainer(MySqlConfig config) {
    super(config);
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
        dropUserIfExists(connection, dbConfig.getUsername());
        dropDatabaseIfExists(connection, dbConfig.getDbName());
      }
      createDatabase(connection);
      createUser(connection);

    } catch (SQLException e) {
      throw new RuntimeException("Error when creating database and role", e);
    }
  }

  private void createUser(Connection connection) {
    createUser(connection, dbConfig.getUsername(), dbConfig.getPassword(), dbConfig.getDbName());
  }


  private void dropDatabaseIfExists(Connection connection, String dbName) {
    if (databaseExists(connection, dbName)) {
      sqlRun(connection, "drop database " + dbName);
    }
  }

  private void dropUserIfExists(Connection connection, String username) {
    if (userExists(connection, username)) {
      sqlRun(connection, "drop user '" + username + "'@'%'");
    }
  }

  private void createUser(Connection connection, String dbUser, String dbPassword, String db) {
    if (!userExists(connection, dbUser)) {
      sqlRun(connection, "create user '" + dbUser + "'@'%' identified by '" + dbPassword + "'");
      sqlRun(connection, "grant all on " + db + ".* to '" + dbUser + "'@'%'");
    }
  }

  private void createDatabase(Connection connection) {
    if (!databaseExists(connection, dbConfig.getDbName())) {
      createDatabase(connection, dbConfig.getDbName());
      if (!dbConfig.version.startsWith("5")) {
        setLogBinTrustFunction(connection);
      }
    }
  }

  private void setLogBinTrustFunction(Connection connection) {
     sqlRun(connection, "set global log_bin_trust_function_creators=1");
  }

  private void createDatabase(Connection connection, String dbName) {
    sqlRun(connection, "create database " + dbName);
  }

  private boolean databaseExists(Connection connection, String dbName) {
    return sqlHasRow(connection, "show databases like '" + dbName + "'");
  }

  private boolean userExists(Connection connection, String dbUser) {
    return sqlHasRow(connection, "select User from user where User = '" + dbUser + "'");
  }

  @Override
  protected ProcessBuilder runProcess() {

    List<String> args = dockerRun();
    if (defined(dbConfig.getAdminPassword())) {
      args.add("-e");
      args.add("MYSQL_ROOT_PASSWORD=" + dbConfig.getAdminPassword());
    }
    args.add(config.getImage());

    if (config.isDefaultCollation()) {
      // leaving it as mysql server default

    } else if (config.isExplicitCollation()) {
      String characterSet = config.getCharacterSet();
      if (characterSet != null) {
        args.add("--character-set-server=" + characterSet);
      }
      String collation = config.getCollation();
      if (collation != null) {
        args.add("--collation-server=" + collation);
      }
    } else {
      args.add("--character-set-server=utf8mb4");
      args.add("--collation-server=utf8mb4_bin");
    }
    if (!dbConfig.version.startsWith("5")) {
      args.add("--default-authentication-plugin=mysql_native_password");
      args.add("--skip-log-bin");
    }
    return createProcessBuilder(args);
  }

}
