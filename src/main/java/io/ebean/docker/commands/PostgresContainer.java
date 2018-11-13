package io.ebean.docker.commands;

import io.ebean.docker.commands.process.ProcessHandler;
import io.ebean.docker.container.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Commands for controlling a postgres docker container.
 * <p>
 * <p>
 * References:
 * </p>
 * <ul>
 * <li>https://github.com/docker-library/postgres/issues/146</li>
 * </ul>
 */
public class PostgresContainer extends DbContainer implements Container {

  /**
   * Create Postgres container with configuration from properties.
   */
  public static PostgresContainer create(String pgVersion, Properties properties) {
    return new PostgresContainer(new PostgresConfig(pgVersion, properties));
  }

  private static final Logger log = LoggerFactory.getLogger(Commands.class);

  /**
   * Create with configuration.
   */
  public PostgresContainer(PostgresConfig config) {
    super(config);
  }

  /**
   * Start the container and wait for it to be ready.
   * <p>
   * This checks if the container is already running.
   * </p>
   * <p>
   * Returns false if the wait for ready was unsuccessful.
   * </p>
   */
  @Override
  public boolean startWithCreate() {
    startMode = Mode.Create;
    startIfNeeded();
    if (!waitForDatabaseReady()) {
      log.warn("Failed waitForDatabaseReady for container {}", config.containerName());
      return false;
    }
    createUser(true);
    createDatabase(true);
    createDatabaseExtensions();
    if (!waitForConnectivity()) {
      log.warn("Failed waiting for connectivity");
      return false;
    }
    return true;
  }

  /**
   * Start with a drop and create of the database and user.
   */
  @Override
  public boolean startWithDropCreate() {
    startMode = Mode.DropCreate;
    startIfNeeded();
    if (!waitForDatabaseReady()) {
      log.warn("Failed waitForDatabaseReady for container {}", config.containerName());
      return false;
    }

    if (!dropDatabaseIfExists() || !dropUserIfExists()) {
      // failed to drop existing db or user
      return false;
    }
    if (!createUser(false) || !createDatabase(false)) {
      // failed to create the db or user
      return false;
    }
    createDatabaseExtensions();
    if (!waitForConnectivity()) {
      log.warn("Failed waiting for connectivity");
      return false;
    }
    return true;
  }

  @Override
  protected boolean isDatabaseAdminReady() {
    return execute("datname", showDatabases());
  }

  /**
   * Return true if the database exists.
   */
  public boolean databaseExists(String dbName) {
    return !hasZeroRows(databaseExistsFor(dbName));
  }

  /**
   * Return true if the database user exists.
   */
  public boolean userExists(String dbUser) {
    return !hasZeroRows(roleExistsFor(dbUser));
  }

  /**
   * Create the database user.
   */
  public boolean createUser(boolean checkExists) {
    String extraDbUser = getExtraDbUser();
    if (isDefined(extraDbUser) && (!checkExists || !userExists(extraDbUser))) {
      if (!createUser(extraDbUser, getWithDefault(dbConfig.getExtraDbPassword(), dbConfig.getDbPassword()))) {
        log.error("Failed to create extra database user " + extraDbUser);
      }
    }
    if (checkExists && userExists(dbConfig.getDbUser())) {
      return true;
    }
    return createUser(dbConfig.getDbUser(), dbConfig.getDbPassword());
  }

  /**
   * Maybe return an extra user to create.
   * <p>
   * The extra user will default to be the same as the extraDB if that is defined.
   * Additionally we don't create an extra user IF it is the same as the main db user.
   */
  private String getExtraDbUser() {
    String extraUser = getWithDefault(dbConfig.getExtraDbUser(), dbConfig.getExtraDb());
    return extraUser != null && !extraUser.equals(dbConfig.getDbUser()) ? extraUser : null;
  }

  private boolean createUser(String user, String pwd) {
    ProcessBuilder pb = createRole(user, pwd);
    return execute("CREATE ROLE", pb, "Failed to create database user");
  }

  /**
   * Create the database with the option of checking if if already exists.
   *
   * @param checkExists When true check the database doesn't already exists
   */
  public boolean createDatabase(boolean checkExists) {
    String extraDb = dbConfig.getExtraDb();
    if (isDefined(extraDb) && (!checkExists || !databaseExists(extraDb))) {
      String extraUser = getWithDefault(getExtraDbUser(), dbConfig.getDbUser());
      if (!createDatabase(extraDb, extraUser, dbConfig.getExtraDbInitSqlFile())) {
        log.error("Failed to create extra database " + extraDb);
      }
    }
    if (checkExists && databaseExists(dbConfig.getDbName())) {
      return true;
    }
    return createDatabase(dbConfig.getDbName(), dbConfig.getDbUser(), dbConfig.getDbInitSqlFile());
  }

  private void runExtraDbInitSql(String dbName, String dbUser, String initSqlFile) {
    if (isDefined(initSqlFile)) {
      File file = new File(initSqlFile);
      if (!file.exists()) {
        file = checkFileResource(initSqlFile);
      }
      if (file == null) {
        log.error("Could not find init SQL file for database " + dbName + ". No file exists at location or resource path for: " + initSqlFile);
      } else {
        runSqlFile(file, dbUser, dbName);
      }
    }
  }

  private File checkFileResource(String sqlFile) {
    try {
      if (!sqlFile.startsWith("/")) {
        sqlFile = "/" + sqlFile;
      }
      URL resource = getClass().getResource(sqlFile);
      if (resource != null) {
        File file = Paths.get(resource.toURI()).toFile();
        if (file.exists()) {
          return file;
        }
      }
    } catch (Exception e) {
      log.error("Failed to obtain File from resource for init SQL file: " + sqlFile, e);
    }
    // not found
    return null;
  }

  private void runSqlFile(File file, String dbUser, String dbName) {

    String fullPath = file.getAbsolutePath();
    if (copyFileToContainer(fullPath, file.getName())) {
      String containerFilePath = "/tmp/" + file.getName();
      ProcessBuilder pb = sqlFileProcess(dbUser, dbName, containerFilePath);
      executeWithout("ERROR", pb, "Error executing init sql file: " + fullPath);
    }
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

  private boolean copyFileToContainer(String sourcePath, String fileName) {
    ProcessBuilder pb = copyFileToContainerProcess(sourcePath, fileName);
    return execute(pb, "Failed to copy file " + fileName + " to container");
  }

  private ProcessBuilder copyFileToContainerProcess(String sourcePath, String fileName) {

    //docker cp /tmp/init-file.sql ut_postgres:/tmp/init-file.sql
    String dest = config.containerName() + ":/tmp/" + fileName;

    List<String> args = new ArrayList<>();
    args.add(config.docker);
    args.add("cp");
    args.add(sourcePath);
    args.add(dest);
    return createProcessBuilder(args);
  }

  private boolean createDatabase(String dbName, String dbUser, String initSqlFile) {
    ProcessBuilder pb = createDb(dbName, dbUser);
    if (execute("CREATE DATABASE", pb, "Failed to create database with owner")) {
      runExtraDbInitSql(dbName, dbUser, initSqlFile);
      return true;
    }
    return false;
  }

  private String getWithDefault(String value, String defaultValue) {
    return value == null ? defaultValue : value;
  }

  /**
   * Create the database extensions if defined.
   */
  public void createDatabaseExtensions() {

    String dbExtn = dbConfig.getDbExtensions();
    if (isDefined(dbExtn)) {
      if (isDefined(dbConfig.getExtraDb())) {
        createDatabaseExtensionsFor(dbExtn, dbConfig.getExtraDb());
      }
      createDatabaseExtensionsFor(dbExtn, dbConfig.getDbName());
    }
  }

  private void createDatabaseExtensionsFor(String dbExtn, String dbName) {
    String[] extns = dbExtn.split(",");
    for (String extension : extns) {
      extension = extension.trim();
      if (!extension.isEmpty()) {
        ProcessHandler.process(createDatabaseExtension(extension, dbName));
      }
    }
  }

  private boolean isDefined(String value) {
    return value != null && !value.isEmpty();
  }

  private ProcessBuilder createDatabaseExtension(String extension, String dbName) {
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
    args.add("-c");
    args.add("create extension if not exists " + extension);

    return createProcessBuilder(args);
  }

  /**
   * Drop the database if it exists.
   */
  public boolean dropDatabaseIfExists() {
    String extraDb = dbConfig.getExtraDb();
    if (isDefined(extraDb) && !dropDatabaseIfExists(extraDb)) {
      log.error("Failed to drop extra database " + extraDb);
    }
    return dropDatabaseIfExists(dbConfig.getDbName());
  }

  private boolean dropDatabaseIfExists(String dbName) {
    if (databaseExists(dbName)) {
      ProcessBuilder pb = dropDatabase(dbName);
      return execute("DROP DATABASE", pb, "Failed to drop database");
    }
    return true;
  }

  /**
   * Drop the database user if it exists.
   */
  public boolean dropUserIfExists() {
    String extraDbUser = getExtraDbUser();
    if (isDefined(extraDbUser) && !dropUserIfExists(extraDbUser)) {
      log.error("Failed to drop extra database user " + extraDbUser);
    }
    return dropUserIfExists(dbConfig.getDbUser());
  }

  private boolean dropUserIfExists(String dbUser) {
    if (!userExists(dbUser)) {
      return true;
    }
    ProcessBuilder pb = dropUser(dbUser);
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

  private ProcessBuilder dropDatabase(String dbName) {
    return sqlProcess("drop database if exists " + dbName);
  }

  private ProcessBuilder dropUser(String dbUser) {
    return sqlProcess("drop role if exists " + dbUser);
  }

  private ProcessBuilder createDb(String dbName, String roleName) {
    return sqlProcess("create database " + dbName + " with owner " + roleName);
  }

  private ProcessBuilder createRole(String roleName, String pass) {
    return sqlProcess("create role " + roleName + " password '" + pass + "' login");//alter role " + roleName + " login;");
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
    args.add(dbConfig.getDbAdminPassword());
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
