package org.avaje.docker.commands;

import org.avaje.docker.commands.process.ProcessHandler;
import org.avaje.docker.container.Container;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MySqlContainer extends DbContainer implements Container {

  public static MySqlContainer create(String mysqlVersion, Properties properties) {
    return new MySqlContainer(new MySqlConfig(mysqlVersion, properties));
  }

  public MySqlContainer(MySqlConfig config) {
    super(config);
  }

  @Override
  protected boolean isDatabaseReady() {
    return commands.logsContain(config.containerName(), "mysqld: ready for connections", "Shutting down");
  }

  /**
   * Check that we can execute admin commands.
   */
  @Override
  protected boolean isDatabaseAdminReady() {
    return execute("Database", showDatabases());
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
      log.warn("Failed waitForDatabaseReady for postgres container {}", config.containerName());
      return false;
    }
    createDatabase(true);
    createUser(true);
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
      log.warn("Failed waitForDatabaseReady for postgres container {}", config.containerName());
      return false;
    }

    dropDatabaseIfExists();
    dropUserIfExists();
    createDatabase(true);
    createUser(true);

    if (!waitForConnectivity()) {
      log.warn("Failed waiting for connectivity");
      return false;
    }
    return true;
  }

  /**
   * Drop the database if it exists.
   */
  private void dropDatabaseIfExists() {
    if (databaseExists()) {
      log.debug("drop database {}", dbConfig.getDbName());
      exec(sqlProcess("drop database "+dbConfig.getDbName(), false), "Failed to drop database");
    }
  }

  /**
   * Drop the database user if it exists.
   */
  private void dropUserIfExists() {
    if (userExists()) {
      log.debug("drop user {}", dbConfig.getDbUser());
      exec(sqlProcess("drop user '"+dbConfig.getDbUser()+"'@'%'", false), "Failed to drop user");
    }
  }

  /**
   * Create the database user.
   */
  public boolean createUser(boolean checkExists) {
    if (checkExists && userExists()) {
      return true;
    }
    log.debug("create postgres user {}", dbConfig.getDbUser());
    createUser(dbConfig.getDbUser(), dbConfig.getDbPassword());
    return true;
  }

  private void createUser(String dbUser, String dbPassword) {

    ProcessBuilder pb = sqlProcess("create user '" + dbUser + "'@'%' identified by '" + dbPassword + "'", false);
    exec(pb, "Failed to create user");

    pb = sqlProcess("grant all on "+dbConfig.getDbName()+".* to '"+dbUser+"'@'%'", false);
    exec(pb, "Failed to create user");
  }

  private void createDatabase(boolean checkExists) {

    if (checkExists && databaseExists()) {
      return;
    }
    log.debug("create mysql database {}", dbConfig.getDbName());
    exec(createDatabase(dbConfig.getDbName()), "Failed to create database");
  }

  private void exec(ProcessBuilder pb, String message) {
    executeWithout("ERROR", pb, message);
  }

  private boolean userExists() {

    return contains(dbUserExists(dbConfig.getDbUser()), dbConfig.getDbUser());
  }

  private boolean databaseExists() {
    return contains(dbExists(dbConfig.getDbName()), dbConfig.getDbName());
  }

  private boolean contains(ProcessBuilder pb, String match) {
    List<String> outLines = ProcessHandler.process(pb).getOutLines();
    return stdoutContains(outLines, match);
  }

  private ProcessBuilder createDatabase(String dbName) {
    return sqlProcess("create database " + dbName, false);
  }

  private ProcessBuilder showDatabases() {
    return sqlProcess("show databases", false);
  }

  private ProcessBuilder dbExists(String dbName) {
    return sqlProcess("show databases like '" + dbName+"'", false);
  }

  private ProcessBuilder dbUserExists(String dbUser) {
    return sqlProcess("select User from user where User = '" + dbUser+"'", true);
  }

  private ProcessBuilder sqlProcess(String sql, boolean withMysql) {
    List<String> args = new ArrayList<>();
    args.add(config.docker);
    args.add("exec");
    args.add("-i");
    args.add(config.containerName());
    args.add("mysql");
    args.add("-uroot");
    args.add("-p"+dbConfig.getDbAdminPassword());
    if (withMysql) {
      args.add("mysql");
    }
    args.add("-e");
    args.add(sql);
//    args.add("\""+sql+"\"");

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

    if (defined(dbConfig.getDbAdminPassword())) {
      args.add("-e");
      args.add("MYSQL_ROOT_PASSWORD=" + dbConfig.getDbAdminPassword());
    }
    args.add(config.getImage());

    return createProcessBuilder(args);
  }

}
