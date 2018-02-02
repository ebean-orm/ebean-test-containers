package org.avaje.docker.commands;

import org.avaje.docker.commands.process.ProcessHandler;
import org.avaje.docker.container.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Commands for controlling a SqlServer docker container.
 */
public class SqlServerContainer extends DbContainer implements Container {

  /**
   * Create Postgres container with configuration from properties.
   */
  public static SqlServerContainer create(String version, Properties properties) {
    return new SqlServerContainer(new SqlServerConfig(version, properties));
  }

  private static final Logger log = LoggerFactory.getLogger(Commands.class);

  /**
   * Create with configuration.
   */
  public SqlServerContainer(SqlServerConfig config) {
    super(config);
  }

  /**
   * Check that we can connect to the DB using SA user.
   */
  @Override
  protected boolean isDatabaseAdminReady() {
    try {
      for (int i = 0; i < config.getMaxReadyAttempts(); i++) {
        try {
          if (hasOneRows(countDatabases())) {
            return true;
          }
        } catch (CommandException e) {
          // can't connect via SA user yet
        }
        Thread.sleep(100);
      }
      return false;

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
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
  @Override
  public boolean startWithCreate() {
    startMode = Mode.Create;
    startIfNeeded();
    if (!waitForDatabaseReady()) {
      log.warn("Failed waitForDatabaseReady for container {}", config.containerName());
      return false;
    }
    if (!createDatabase(true)) {
      return false;
    }
    if (!createLogin(true)) {
      return false;
    }
    if (!createUser(true)) {
      return false;
    }
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

    dropDatabaseIfExists();
    dropLoginIfExists();
    if (!createDatabase(false)) {
      return false;
    }
    if (!createLogin(false)) {
      return false;
    }
    if (!createUser(false)) {
      return false;
    }
    if (!waitForConnectivity()) {
      log.warn("Failed waiting for connectivity");
      return false;
    }
    return true;
  }

  /**
   * Start the container only without creating database, user, extensions etc.
   */
  @Override
  public boolean startContainerOnly() {
    startMode = Mode.Container;
    startIfNeeded();
    if (!waitForDatabaseReady()) {
      log.warn("Failed waitForDatabaseReady for container {}", config.containerName());
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
    return !hasZeroRows(databaseExists(dbConfig.getDbName()));
  }

  /**
   * Return true if the database user exists.
   */
  public boolean loginExists() {
    return !hasZeroRows(loginExists(dbConfig.getDbUser()));
  }

  /**
   * Return true if the database user exists.
   */
  public boolean userExists() {
    return !hasZeroRows(userExists(dbConfig.getDbUser(), dbConfig.getDbName()));
  }

  /**
   * Create the database user.
   */
  public boolean createLogin(boolean checkExists) {
    if (checkExists && loginExists()) {
      return true;
    }
    log.debug("create login {}", dbConfig.getDbUser());
    return execute(createLogin(dbConfig.getDbUser(), dbConfig.getDbPassword()), "Failed to create DB login");
  }

  /**
   * Create the database user.
   */
  public boolean createUser(boolean checkExists) {
    if (checkExists && userExists()) {
      return true;
    }
    log.debug("create user {}", dbConfig.getDbUser());
    boolean success = execute(createUser(dbConfig.getDbUser(), dbConfig.getDbUser(), dbConfig.getDbName()), "Failed to create DB user");
    if (success) {
      success = execute(grantDbOwner(dbConfig.getDbUser(), dbConfig.getDbName()), "Failed to grant DB owner to user");
    }
    return success;
  }

  /**
   * Create the database with the option of checking if if already exists.
   *
   * @param checkExists When true check the database doesn't already exists
   */
  public boolean createDatabase(boolean checkExists) {
    if (checkExists && databaseExists()) {
      return true;
    }
    log.debug("create database {}", dbConfig.getDbName());
    return execute(createDatabase(dbConfig.getDbName()), "Failed to create DB");
  }

  /**
   * Drop the database if it exists.
   */
  public boolean dropDatabaseIfExists() {
    if (!databaseExists()) {
      return true;
    }
    log.debug("drop database {}", dbConfig.getDbName());
    return execute(dropDatabase(dbConfig.getDbName()), "Failed to drop DB");
  }

  /**
   * Drop the database user if it exists.
   */
  public boolean dropLoginIfExists() {
    if (!loginExists()) {
      return true;
    }
    log.debug("drop login {}", dbConfig.getDbUser());
    return execute(dropLogin(dbConfig.getDbUser()), "Failed to drop DB login");
  }

  @Override
  protected boolean isDatabaseReady() {
    return logsContain("SQL Server is now ready");
  }

  private ProcessBuilder dropDatabase(String dbName) {
    return sqlProcess("drop database " + dbName);
  }

  private ProcessBuilder dropUser(String dbUser, String withDb) {
    return sqlProcess("drop user " + dbUser, withDb);
  }

  private ProcessBuilder dropLogin(String dbLogin) {
    return sqlProcess("drop login " + dbLogin);
  }

  private ProcessBuilder createDatabase(String dbName) {
    return sqlProcess("create database " + dbName);
  }

  private ProcessBuilder createLogin(String login, String pass) {
    return sqlProcess("create login " + login + " with password = '" + pass + "'");
  }

  private ProcessBuilder createUser(String roleName, String login, String withDb) {
    return sqlProcess("create user " + roleName + " for login " + login, withDb);
  }

  private ProcessBuilder grantDbOwner(String roleName, String withDb) {
    return sqlProcess("EXEC sp_addrolemember 'db_owner', " + roleName, withDb);
  }

  private ProcessBuilder userExists(String userName, String withDb) {
    return sqlProcess("select 1 from sys.database_principals where name = '" + userName + "'", withDb);
  }

  private ProcessBuilder loginExists(String roleName) {
    return sqlProcess("select 1 from master.dbo.syslogins where loginname = '" + roleName + "'");
  }

  private ProcessBuilder databaseExists(String dbName) {
    return sqlProcess("select 1 from sys.databases where name='" + dbName + "'");
  }

  private ProcessBuilder countDatabases() {
    return sqlProcess("select count(*) from sys.databases");
  }

  private ProcessBuilder sqlProcess(String sql) {
    return sqlProcess(sql, null);
  }

  private ProcessBuilder sqlProcess(String sql, String withDb) {
    List<String> args = new ArrayList<>();
    args.add(config.docker);
    args.add("exec");
    args.add("-i");
    args.add(config.containerName());
    args.add("/opt/mssql-tools/bin/sqlcmd");
    if (withDb != null) {
      args.add("-d");
      args.add(withDb);
    }
    args.add("-U");
    args.add("sa");
    args.add("-P");
    args.add(dbConfig.getDbAdminPassword());
    args.add("-Q");
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

    args.add("-e");
    args.add("ACCEPT_EULA=Y");

    args.add("-e");
    args.add("SA_PASSWORD=" + dbConfig.getDbAdminPassword());//+"'");
    args.add(config.getImage());

    return createProcessBuilder(args);
  }

  private boolean hasZeroRows(ProcessBuilder pb) {
    return hasZeroRows(ProcessHandler.process(pb).getStdOutLines());
  }

  private boolean hasOneRows(ProcessBuilder pb) {
    return hasOneRows(ProcessHandler.process(pb).getStdOutLines());
  }

  private boolean hasZeroRows(List<String> stdOutLines) {
    return stdoutContains(stdOutLines, "(0 rows affected)");
  }

  private boolean hasOneRows(List<String> stdOutLines) {
    return stdoutContains(stdOutLines, "(1 rows affected)");
  }

}
