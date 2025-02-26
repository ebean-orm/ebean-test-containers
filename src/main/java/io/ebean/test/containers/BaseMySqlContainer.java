package io.ebean.test.containers;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract class BaseMySqlContainer<C extends BaseMySqlContainer<C>> extends BaseJdbcContainer<C> {

  BaseMySqlContainer(BaseDbBuilder<?, ?> config) {
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
        for (String extraDb : toDatabaseNames(dbConfig.getExtraDb())) {
          dropDatabaseIfExists(connection, extraDb);
        }
      }
      createDatabase(connection);
      createUser(connection);

    } catch (SQLException e) {
      throw new RuntimeException("Error when creating database and role", e);
    }
  }

  private void dropDatabaseIfExists(Connection connection, String dbName) {
    if (notEmpty(dbName) && databaseExists(connection, dbName)) {
      sqlRun(connection, "drop database " + dbName);
    }
  }

  private void dropUserIfExists(Connection connection, String username) {
    if (userExists(connection, username)) {
      sqlRun(connection, "drop user '" + username + "'@'%'");
    }
  }

  private void createUser(Connection connection) {
    if (!userExists(connection, dbConfig.getUsername())) {
      sqlRun(connection, "create user '" + dbConfig.getUsername() + "'@'%' identified by '" + dbConfig.getPassword() + "'");
      sqlRun(connection, "grant all on " + dbConfig.getDbName() + ".* to '" + dbConfig.getUsername() + "'@'%'");
      for (String extraDb : toDatabaseNames(dbConfig.getExtraDb())) {
        sqlRun(connection, "grant all on " + extraDb + ".* to '" + dbConfig.getUsername() + "'@'%'");
      }
    }
  }

  private void createDatabase(Connection connection) {
    if (!databaseExists(connection, dbConfig.getDbName())) {
      createDatabase(connection, dbConfig.getDbName());
      createExtraDatabases(connection);
      if (!dbConfig.version().startsWith("5")) {
        setLogBinTrustFunction(connection);
      }
    }
  }

  private void createExtraDatabases(Connection connection) {
    for (String extraDb : toDatabaseNames(dbConfig.getExtraDb())) {
      createDatabase(connection, extraDb);
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

    if (dbConfig.isDefaultCollation()) {
      // leaving it as mysql server default

    } else if (dbConfig.isExplicitCollation()) {
      String characterSet = dbConfig.getCharacterSet();
      if (characterSet != null) {
        args.add("--character-set-server=" + characterSet);
      }
      String collation = dbConfig.getCollation();
      if (collation != null) {
        args.add("--collation-server=" + collation);
      }
    } else {
      args.add("--character-set-server=utf8mb4");
      args.add("--collation-server=utf8mb4_bin");
    }
    if (!dbConfig.version().startsWith("5")) {
      args.add("--default-authentication-plugin=mysql_native_password");
      args.add("--skip-log-bin");
    }
    return createProcessBuilder(args);
  }

  static List<String> toDatabaseNames(String dbNames) {
    if (dbNames == null) {
      return Collections.emptyList();
    }
    String[] names = dbNames.split(",");
    List<String> dbNameList = new ArrayList<>();
    for (String name : names) {
      name = name.trim();
      if (!name.isEmpty()) {
        dbNameList.add(name);
      }
    }
    return dbNameList;
  }
}
