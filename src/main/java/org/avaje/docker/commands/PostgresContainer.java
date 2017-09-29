package org.avaje.docker.commands;

import org.avaje.docker.commands.process.ProcessHandler;
import org.avaje.docker.commands.process.ProcessResult;
import org.avaje.docker.container.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

  private static final Logger log = LoggerFactory.getLogger(Commands.class);

  public PostgresContainer(PostgresConfig config) {
    super(config);
  }

  @Override
  public String getStartDescription() {
    return config.startDescription();
  }

  @Override
  public String getStopDescription() {
    return config.stopDescription();
  }

  /**
   * Start with a mode of 'create', 'dropCreate' or 'container'.
   * <p>
   * Expected that mode create will be best most of the time.
   */
  public boolean start() {//Consumer<String> log) {

//    if (log) {
//      log.a
//    }

    String mode = config.getStartMode().toLowerCase().trim();
    switch (mode) {
      case "create":
        return startWithCreate();
      case "dropcreate":
        return startWithDropCreate();
      case "container":
        return startContainerOnly();
      default:
        return startWithCreate();
    }
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
  public boolean startWithCreate() {
    startIfNeeded();
    if (!waitForDatabaseReady()) {
      log.warn("Failed waitForDatabaseReady for postgres container {}", config.containerName());
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
  public boolean startWithDropCreate() {
    startIfNeeded();
    if (!waitForDatabaseReady()) {
      log.warn("Failed waitForDatabaseReady for postgres container {}", config.containerName());
      return false;
    }

    dropDatabaseIfExists();
    dropUserIfExists();
    createUser(false);
    createDatabase(false);
    createDatabaseExtensions();

    if (!waitForConnectivity()) {
      log.warn("Failed waiting for connectivity");
      return false;
    }
    return true;
  }

  /**
   * Start the container only without creating database, user, extensions etc.
   */
  public boolean startContainerOnly() {
    startIfNeeded();
    if (!waitForDatabaseReady()) {
      log.warn("Failed waitForDatabaseReady for postgres container {}", config.containerName());
      return false;
    }

    if (!waitForConnectivity()) {
      log.warn("Failed waiting for connectivity");
      return false;
    }
    return true;
  }

  /**
   * Return true if the database exists.
   */
  public boolean databaseExists() {
    return !hasZeroRows(databaseExists(config.getDbName()));
  }

  /**
   * Return true if the database user exists.
   */
  public boolean userExists() {
    return !hasZeroRows(roleExists(config.getDbUser()));
  }

  /**
   * Create the database user.
   */
  public boolean createUser(boolean checkExists) {
    if (!userDefined() || (checkExists && userExists())) {
      return false;
    }
    log.debug("create postgres user {}", config.containerName());
    ProcessBuilder pb = createRole(config.getDbUser(), config.getDbPassword());
    List<String> stdOutLines = ProcessHandler.process(pb).getStdOutLines();
    return stdOutLines.size() == 2;
  }

  /**
   * Create the database with the option of checking if if already exists.
   *
   * @param checkExists When true check the database doesn't already exists
   */
  public boolean createDatabase(boolean checkExists) {
    if (!databaseDefined() || (checkExists && databaseExists())) {
      return false;
    }
    log.debug("create postgres database {} with owner {}", config.getDbName(), config.getDbUser());
    ProcessBuilder pb = createDatabase(config.getDbName(), config.getDbUser());
    List<String> stdOutLines = ProcessHandler.process(pb).getStdOutLines();
    return stdOutLines.size() == 2;
  }

  /**
   * Create the database extensions if defined.
   */
  public void createDatabaseExtensions() {

    String dbExtn = config.getDbExtensions();
    if (isDefined(dbExtn)) {
      log.debug("create database extensions {}", dbExtn);
      String[] extns = dbExtn.split(",");
      for (String extension : extns) {
        ProcessHandler.process(createDatabaseExtension(extension));
      }
    }
  }

  private boolean isDefined(String value) {
    return value != null && !value.isEmpty();
  }

  private ProcessBuilder createDatabaseExtension(String extension) {
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
    args.add(config.getDbName());
    args.add("-c");
    args.add("create extension if not exists " + extension);

    return createProcessBuilder(args);
  }

  /**
   * Drop the database if it exists.
   */
  public boolean dropDatabaseIfExists() {
    if (!databaseDefined() || !databaseExists()) {
      return false;
    }
    log.debug("drop postgres database {}", config.getDbName());
    ProcessBuilder pb = dropDatabase(config.getDbName());
    List<String> stdOutLines = ProcessHandler.process(pb).getStdOutLines();
    return stdOutLines.size() == 1;
  }

  /**
   * Drop the database user if it exists.
   */
  public boolean dropUserIfExists() {

    if (!userDefined() || !userExists()) {
      return false;
    }
    log.debug("drop postgres user {}", config.getDbUser());
    ProcessBuilder pb = dropUser(config.getDbUser());
    List<String> stdOutLines = ProcessHandler.process(pb).getStdOutLines();
    return stdOutLines.size() == 1;
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

    ProcessBuilder pb = pgIsReady();
    try {
      ProcessResult result = ProcessHandler.process(pb.start());
      return result.success();
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Return true when the DB is ready for taking commands (like create database, user etc).
   */
  public boolean waitForDatabaseReady() {
    try {
      for (int i = 0; i < config.getMaxReadyAttempts(); i++) {
        if (isDatabaseReady()) {
          return true;
        }
        Thread.sleep(100);
      }
      return false;

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private boolean hasZeroRows(ProcessBuilder pb) {
    return hasZeroRows(ProcessHandler.process(pb).getStdOutLines());
  }

  private ProcessBuilder dropDatabase(String dbName) {
    return sqlProcess("drop database if exists " + dbName);
  }

  private ProcessBuilder dropUser(String dbUser) {
    return sqlProcess("drop role if exists " + dbUser);
  }

  private ProcessBuilder createDatabase(String dbName, String roleName) {
    return sqlProcess("create database " + dbName + " with owner " + roleName);
  }

  private ProcessBuilder createRole(String roleName, String pass) {
    return sqlProcess("create role " + roleName + " password '" + pass + "' login");//alter role " + roleName + " login;");
  }

  private ProcessBuilder roleExists(String roleName) {
    return sqlProcess("select rolname from pg_roles where rolname = '" + roleName + "'");
  }

  private ProcessBuilder databaseExists(String dbName) {
    return sqlProcess("select 1 from pg_database where datname = '" + dbName + "'");
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

    if (config.isInMemory() && config.getTmpfs() != null) {
      args.add("--tmpfs");
      args.add(config.getTmpfs());
    }

    args.add("-e");
    args.add(config.getDbAdminPassword());
    args.add("-d");
    args.add(config.getImage());

    return createProcessBuilder(args);
  }

  private ProcessBuilder pgIsReady() {

    // not depending on locally installed pg_isready

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
    if (stdOutLines.size() < 2) {
      throw new RuntimeException("Unexpected results - lines:" + stdOutLines);
    }
    return stdOutLines.get(2).equals("(0 rows)");
  }

}
