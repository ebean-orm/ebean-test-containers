package io.ebean.test.containers;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Commands for controlling a postgres docker container.
 * <p>
 * References: <a href="https://github.com/docker-library/postgres/issues/146">docker-library/postgres/issues/146</a>
 */
abstract class BasePostgresContainer<C extends BasePostgresContainer<C>> extends BaseJdbcContainer<C> {

  BasePostgresContainer(BaseDbBuilder<?, ?> config) {
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
        dropDatabaseIfExists(connection, dbConfig.getDbName());
        dropRoleIfExists(connection, dbConfig.getUsername());
      }
      if (databaseNotExists(connection, dbConfig.getDbName())) {
        createExtraDb(connection, withDrop);
        createRole(connection);
        createDatabase(connection);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error when creating database and role", e);
    }
  }

  private void dropRoleIfExists(Connection connection, String username) {
    sqlRun(connection, "drop role if exists " + username);
  }

  private void dropDatabaseIfExists(Connection connection, String dbName) {
    sqlRun(connection, "drop database if exists " + dbName);
  }

  private void createExtraDb(Connection connection, boolean withDrop) {
    final String extraUser = getExtraDbUser();
    if (defined(extraUser)) {
      final String extraDb = dbConfig.getExtraDb();
      if (withDrop) {
        dropDatabaseIfExists(connection, extraDb);
        dropRoleIfExists(connection, extraUser);
      }
      createRole(connection, extraUser, getWithDefault(dbConfig.getExtraDbPassword(), dbConfig.getPassword()));
      if (databaseNotExists(connection, extraDb)) {
        createExtraDatabase(connection, extraDb, extraUser);
      }
    }
  }

  private void createDatabase(Connection connection) {
    createDatabaseWithOwner(connection, dbConfig.getDbName(), dbConfig.getUsername());
    addExtensions(dbConfig.getExtensions(), dbConfig.jdbcUrl());
    createDatabaseInitSql(dbConfig.getDbName(), dbConfig.getUsername(),  dbConfig.getInitSqlFile(), dbConfig.getSeedSqlFile());
  }

  private void createExtraDatabase(Connection connection, String extraDb, String extraUser) {
    createDatabaseWithOwner(connection, extraDb, extraUser);
    addExtensions(dbConfig.getExtraDbExtensions(), dbConfig.jdbcExtraUrl());
    createDatabaseInitSql(extraDb, extraUser,  dbConfig.getExtraDbInitSqlFile(), dbConfig.getExtraDbSeedSqlFile());
  }

  private void createRole(Connection connection) {
    createRole(connection, dbConfig.getUsername(), dbConfig.getPassword());
  }

  private void createRole(Connection connection, String username, String password) {
    if (!sqlHasRow(connection, "select rolname from pg_roles where rolname = '" + username + "'")) {
      sqlRun(connection, "create role " + username + " password '" + password + "' login createrole superuser");
    }
  }

  private boolean databaseNotExists(Connection connection, String dbName) {
    return !sqlHasRow(connection, "select 1 from pg_database where datname = '" + dbName + "'");
  }

  private void createDatabaseWithOwner(Connection connection, String dbName, String owner) {
    sqlRun(connection, "create database " + dbName + " with owner " + owner);
  }

  private void createDatabaseInitSql(String dbName, String owner, String initSql, String seedSql) {
    if (defined(initSql)) {
      runDbSqlFile(dbName, owner, initSql);
    }
    if (defined(seedSql)) {
      runDbSqlFile(dbName, owner, seedSql);
    }
  }

  private void addExtensions(String dbExtensions, String jdbcUrl) {
    if (!defined(dbExtensions)) {
      return;
    }
    final List<String> extensions = parseExtensions(dbExtensions);
    if (!extensions.isEmpty()) {
      try (Connection connection = dbConfig.createAdminConnection(jdbcUrl)) {
        for (String extension : extensions) {
          sqlRun(connection, "create extension if not exists \"" + extension + "\"");
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Maybe return an extra user to create.
   * <p>
   * The extra user will default to be the same as the extraDB if that is defined.
   * Additionally we don't create an extra user IF it is the same as the main db user.
   */
  private String getExtraDbUser() {
    String extraUser = getWithDefault(dbConfig.getExtraDbUser(), dbConfig.getExtraDb());
    return extraUser != null && !extraUser.equals(dbConfig.getUsername()) ? extraUser : null;
  }

  @Override
  protected void executeSqlFile(String dbUser, String dbName, String containerFilePath) {
    ProcessBuilder pb = sqlFileProcess(dbUser, dbName, containerFilePath);
    executeWithout("ERROR", pb, "Error executing init sql file: " + containerFilePath);
  }

  private ProcessBuilder sqlFileProcess(String dbUser, String dbName, String containerFilePath) {
    List<String> args = execPsql();
    args.add(dbUser);
    args.add("-d");
    args.add(dbName);
    args.add("-f");
    args.add(containerFilePath);
    return createProcessBuilder(args);
  }

  private String getWithDefault(String value, String defaultValue) {
    return value == null ? defaultValue : value;
  }

  private List<String> parseExtensions(String dbExtn) {
    return TrimSplit.split(dbExtn);
  }

  private List<String> execPsql() {
    List<String> args = new ArrayList<>();
    args.add(config.docker());
    args.add("exec");
    args.add("-i");
    args.add(config.containerName());
    args.add("psql");
    args.add("-U");
    return args;
  }

  @Override
  protected ProcessBuilder runProcess() {
    List<String> args = dockerRun();
    if (dbConfig.isInMemory() && dbConfig.getTmpfs() != null) {
      args.add("--tmpfs");
      args.add(dbConfig.getTmpfs());
    }
    if (!dbConfig.getAdminPassword().isEmpty()) {
      args.add("-e");
      args.add("POSTGRES_PASSWORD=" + dbConfig.getAdminPassword());
    }
    args.add(config.getImage());
    return createProcessBuilder(args);
  }
}
