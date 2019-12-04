package io.ebean.docker.commands;

import io.ebean.docker.commands.process.ProcessHandler;
import io.ebean.docker.container.Container;

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
    if (startIfNeeded() &&  fastStart()) {
      // container was running, fast start enabled and passed
      // so skip the usual checks for user, extensions and connectivity
      return true;
    }
    if (!waitForDatabaseReady()) {
      log.warn("Failed waitForDatabaseReady for container {}", config.containerName());
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
      log.warn("Failed waitForDatabaseReady for container {}", config.containerName());
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

  @Override
  protected boolean isFastStartDatabaseExists() {
    return databaseExists();
  }

  /**
   * Drop the database if it exists.
   */
  private void dropDatabaseIfExists() {
    if (databaseExists()) {
      log.debug("drop database {}", dbConfig.getDbName());
      exec(sqlProcess("drop database " + dbConfig.getDbName(), false), "Failed to drop database");
    }
  }

  /**
   * Drop the database user if it exists.
   */
  private void dropUserIfExists() {
    if (userExists()) {
      log.debug("drop user {}", dbConfig.getUsername());
      exec(sqlProcess("drop user '" + dbConfig.getUsername() + "'@'%'", false), "Failed to drop user");
    }
  }

  /**
   * Create the database user.
   */
  public boolean createUser(boolean checkExists) {
    if (checkExists && userExists()) {
      return true;
    }
    log.debug("create user {}", dbConfig.getUsername());
    createUser(dbConfig.getUsername(), dbConfig.getPassword());
    return true;
  }

  private void createUser(String dbUser, String dbPassword) {

    ProcessBuilder pb = sqlProcess("create user '" + dbUser + "'@'%' identified by '" + dbPassword + "'", false);
    exec(pb, "Failed to create user");

    pb = sqlProcess("grant all on " + dbConfig.getDbName() + ".* to '" + dbUser + "'@'%'", false);
    exec(pb, "Failed to create user");
  }

  private void createDatabase(boolean checkExists) {

    if (checkExists && databaseExists()) {
      return;
    }
    log.debug("create database {}", dbConfig.getDbName());
    exec(createDatabase(dbConfig.getDbName()), "Failed to create database");
    if (!dbConfig.version.startsWith("5")) {
      exec(setLogBinTrustFunction(), "Failed to set log_bin_trust_function_creators");
    }
  }

  private void exec(ProcessBuilder pb, String message) {
    executeWithout("ERROR", pb, message);
  }

  private boolean userExists() {

    return contains(dbUserExists(dbConfig.getUsername()), dbConfig.getUsername());
  }

  private boolean databaseExists() {
    return contains(dbExists(dbConfig.getDbName()), dbConfig.getDbName());
  }

  private boolean contains(ProcessBuilder pb, String match) {
    List<String> outLines = ProcessHandler.process(pb).getOutLines();
    return stdoutContains(outLines, match);
  }

  private ProcessBuilder setLogBinTrustFunction() {
    return sqlProcess("set global log_bin_trust_function_creators=1", false);
  }

  private ProcessBuilder createDatabase(String dbName) {
    return sqlProcess("create database " + dbName, false);
  }

  private ProcessBuilder showDatabases() {
    return sqlProcess("show databases", false);
  }

  private ProcessBuilder dbExists(String dbName) {
    return sqlProcess("show databases like '" + dbName + "'", false);
  }

  private ProcessBuilder dbUserExists(String dbUser) {
    return sqlProcess("select User from user where User = '" + dbUser + "'", true);
  }

  private ProcessBuilder sqlProcess(String sql, boolean withMysql) {
    List<String> args = new ArrayList<>();
    args.add(config.docker);
    args.add("exec");
    args.add("-i");
    args.add(config.containerName());
    args.add("mysql");
    args.add("-uroot");
    args.add("-p" + dbConfig.getAdminPassword());
    if (withMysql) {
      args.add("mysql");
    }
    args.add("-e");
    args.add(sql);

    return createProcessBuilder(args);
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
    }

    return createProcessBuilder(args);
  }

}
