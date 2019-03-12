package io.ebean.docker.commands;

import io.ebean.docker.commands.process.ProcessHandler;
import io.ebean.docker.container.Container;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Commands for controlling a postgres docker container.
 * <p>
 * References:
 * </p>
 * <ul>
 * <li>https://github.com/docker-library/postgres/issues/146</li>
 * </ul>
 */
public class PostgresContainer extends BaseDbContainer implements Container {

  /**
   * Create Postgres container with configuration from properties.
   */
  public static PostgresContainer create(String pgVersion, Properties properties) {
    return new PostgresContainer(new PostgresConfig(pgVersion, properties));
  }

  /**
   * Create with configuration.
   */
  public PostgresContainer(PostgresConfig config) {
    super(config);
  }

  @Override
  protected boolean isDatabaseAdminReady() {
    return execute("datname", showDatabases());
  }

  @Override
  protected boolean isFastStartDatabaseExists() {
    return databaseExists(dbConfig.getDbName());
  }

  /**
   * Return true if the database exists.
   */
  @Override
  public boolean databaseExists(String dbName) {
    return !hasZeroRows(databaseExistsFor(dbName));
  }

  /**
   * Return true if the database user exists.
   */
  @Override
  public boolean userExists(String dbUser) {
    return !hasZeroRows(roleExistsFor(dbUser));
  }

  @Override
  protected boolean createUser(String user, String pwd) {
    ProcessBuilder pb = createRole(user, pwd);
    return execute("CREATE ROLE", pb, "Failed to create database user");
  }

  @Override
  protected void executeSqlFile(String dbUser, String dbName, String containerFilePath) {
    ProcessBuilder pb = sqlFileProcess(dbUser, dbName, containerFilePath);
    executeWithout("ERROR", pb, "Error executing init sql file: " + containerFilePath);
  }

  private ProcessBuilder sqlFileProcess(String dbUser, String dbName, String containerFilePath) {
    List<String> args = new ArrayList<>();
    args.add(config.docker);
    args.add("exec");
    args.add("-i");
    args.add(config.containerName());
    args.add("psql");
    args.add("-U");
    args.add(dbUser);
    args.add("-d");
    args.add(dbName);
    args.add("-f");
    args.add(containerFilePath);
    return createProcessBuilder(args);
  }

  @Override
  protected boolean createDatabase(String dbName, String dbUser, String initSqlFile, String seedSqlFile) {
    ProcessBuilder pb = createDb(dbName, dbUser);
    if (execute("CREATE DATABASE", pb, "Failed to create database with owner")) {
      runDbSqlFile(dbName, dbUser, initSqlFile);
      runDbSqlFile(dbName, dbUser, seedSqlFile);
      return true;
    }
    return false;
  }

  private String getWithDefault(String value, String defaultValue) {
    return value == null ? defaultValue : value;
  }

  @Override
  protected void createDatabaseExtensionsFor(String dbExtn, String dbName) {

    List<String> extensions = new ArrayList<>();
    for (String extension : dbExtn.split(",")) {
      extension = extension.trim();
      if (!extension.isEmpty()) {
        extensions.add(extension);
      }
    }
    if (!extensions.isEmpty()) {
      ProcessHandler.process(createDatabaseExtension(extensions, dbName));
    }
  }

  private ProcessBuilder createDatabaseExtension(List<String> extensions, String dbName) {
    //docker exec -i ut_postgres psql -U postgres -d test_db -c "create extension if not exists pgcrypto";
    List<String> args = new ArrayList<>();
    args.add(config.docker);
    args.add("exec");
    args.add("-i");
    args.add(config.containerName());
    args.add("psql");
    args.add("-U");
    args.add("postgres");
    args.add("-d");
    args.add(dbName);
    for (String extension : extensions) {
      args.add("-c");
      args.add("create extension if not exists " + extension);
    }

    return createProcessBuilder(args);
  }

  @Override
  protected boolean dropDatabase(String dbName) {
    ProcessBuilder pb = sqlProcess("drop database if exists " + dbName);
    return execute("DROP DATABASE", pb, "Failed to drop database");
  }

  @Override
  protected boolean dropUser(String dbUser) {
    ProcessBuilder pb = sqlProcess("drop role if exists " + dbUser);
    return execute("DROP ROLE", pb, "Failed to drop database user");
  }

  /**
   * Wait for the 'database system is ready' using pg_isready.
   * <p>
   * This means the DB is ready to take server side commands but TCP connectivity may still not be available yet.
   * </p>
   *
   * @return True when we detect the database is ready (to create user and database etc).
   */
  public boolean isDatabaseReady() {
    try {
      return ProcessHandler.process(pgIsReady()).success();
    } catch (RuntimeException e) {
      return false;
    }
  }

  private boolean hasZeroRows(ProcessBuilder pb) {
    return hasZeroRows(ProcessHandler.process(pb).getOutLines());
  }

  private ProcessBuilder createDb(String dbName, String roleName) {
    return sqlProcess("create database " + dbName + " with owner " + roleName);
  }

  private ProcessBuilder createRole(String roleName, String pass) {
    return sqlProcess("create role " + roleName + " password '" + pass + "' login createrole");
  }

  private ProcessBuilder roleExistsFor(String roleName) {
    return sqlProcess("select rolname from pg_roles where rolname = '" + roleName + "'");
  }

  private ProcessBuilder databaseExistsFor(String dbName) {
    return sqlProcess("select 1 from pg_database where datname = '" + dbName + "'");
  }

  private ProcessBuilder showDatabases() {
    return sqlProcess("select datname from pg_database");
  }

  private ProcessBuilder sqlProcess(String sql) {
    List<String> args = new ArrayList<>();
    args.add(config.docker);
    args.add("exec");
    args.add("-i");
    args.add(config.containerName());
    args.add("psql");
    args.add("-U");
    args.add("postgres");
    args.add("-c");
    args.add(sql);
    return createProcessBuilder(args);
  }

  @Override
  protected ProcessBuilder runProcess() {

    List<String> args = new ArrayList<>();
    args.add(config.docker);
    args.add("run");
    args.add("-d");
    args.add("--name");
    args.add(config.containerName());
    args.add("-p");
    args.add(config.getPort() + ":" + config.getInternalPort());

    if (dbConfig.isInMemory() && dbConfig.getTmpfs() != null) {
      args.add("--tmpfs");
      args.add(dbConfig.getTmpfs());
    }

    args.add("-e");
    args.add(dbConfig.getAdminPassword());
    args.add(config.getImage());

    return createProcessBuilder(args);
  }

  private ProcessBuilder pgIsReady() {

    List<String> args = new ArrayList<>();

    args.add(config.docker);
    args.add("exec");
    args.add("-i");
    args.add(config.containerName());
    args.add("pg_isready");
    args.add("-h");
    args.add("localhost");
    args.add("-p");
    args.add(config.getInternalPort());

    return createProcessBuilder(args);
  }

  private boolean hasZeroRows(List<String> stdOutLines) {
    return stdoutContains(stdOutLines, "(0 rows)");
  }

}
