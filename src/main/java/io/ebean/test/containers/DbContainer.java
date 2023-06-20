package io.ebean.test.containers;

import io.ebean.test.containers.process.ProcessHandler;

import java.io.File;
import java.lang.System.Logger.Level;
import java.net.URL;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

abstract class DbContainer extends BaseContainer implements Container {

  final InternalConfigDb dbConfig;
  boolean checkConnectivityUsingAdmin;
  int conditionPauseMillis = 100;

  DbContainer(DbConfig<?, ?> config) {
    super(config);
    this.dbConfig = config.internalConfig();
  }

  /**
   * Log that the container is already running.
   */
  void logRunning() {
    log.log(Level.INFO, "Container {0} running with {1} shutdownMode:{2}", logContainerName(), dbConfig.summary(), logContainerShutdown());
  }

  @Override
  void logRun() {
    log.log(Level.INFO, "Run container {0} with {1} shutdownMode:{2}", logContainerName(), dbConfig.summary(), logContainerShutdown());
  }

  @Override
  void logStart() {
    log.log(Level.INFO, "Start container {0} with {1} shutdownMode:{2}", logContainerName(), dbConfig.summary(), logContainerShutdown());
  }

  /**
   * Return the JDBC url to connect to this container.
   */
  public String jdbcUrl() {
    return config.jdbcUrl();
  }

  /**
   * Create a connection to this database container.
   */
  public Connection createConnection() throws SQLException {
    return config.createConnection();
  }

  @Override
  public boolean startMaybe() {
    setDefaultContainerName();
    return shutdownHook(logStarted(startForMode()));
  }

  /**
   * Start with a mode of 'create', 'dropCreate' or 'container'.
   * <p>
   * Expected that mode create will be best most of the time.
   */
  protected boolean startForMode() {
    switch (config.getStartMode()) {
      case DropCreate:
        return startWithDropCreate();
      case Container:
        return startContainerOnly();
      default:
        return startWithCreate();
    }
  }

  /**
   * Start the DB container ensuring the DB and user exist creating them if necessary.
   */
  public boolean startWithCreate() {
    return startWithConnectivity();
  }

  /**
   * Start the DB container ensuring the DB and user are dropped and then created.
   */
  public boolean startWithDropCreate() {
    return startWithConnectivity();
  }

  /**
   * Start the container only without creating database, user, extensions etc.
   */
  public boolean startContainerOnly() {
    startIfNeeded();
    if (!waitForDatabaseReady()) {
      log.log(Level.ERROR, "Failed waitForDatabaseReady for container {0}", config.containerName());
      return false;
    }

    if (!waitForConnectivity()) {
      log.log(Level.ERROR, "Failed waiting for connectivity for {0}", config.containerName());
      return false;
    }
    return true;
  }

  /**
   * If we are using FastStartMode just check is the DB exists and if so assume it is all created correctly.
   * <p>
   * This should only be used with Mode.Create and when the container is already running.
   */
  protected boolean fastStart() {
    if (!dbConfig.isFastStartMode()) {
      return false;
    }
    try {
      return isFastStartDatabaseExists();
    } catch (CommandException e) {
      log.log(Level.DEBUG, "failed fast start check - using normal startup");
      return false;
    }
  }

  protected boolean isFastStartDatabaseExists() {
    // return false as by default it is not supported
    return false;
  }

  /**
   * Return the ProcessBuilder used to execute the container run command.
   */
  protected abstract ProcessBuilder runProcess();

  /**
   * Return true when the database is ready to take commands.
   */
  protected abstract boolean isDatabaseReady();

  /**
   * Return true when the database is ready to take admin commands.
   */
  protected abstract boolean isDatabaseAdminReady();

  protected void executeSqlFile(String dbUser, String dbName, String containerFilePath) {
    throw new RuntimeException("executeSqlFile is Not implemented for this platform - Postgres only at this stage");
  }

  /**
   * Return true when the DB is ready for taking commands (like create database, user etc).
   */
  public boolean waitForDatabaseReady() {
    return conditionLoop(this::isDatabaseReady)
      && conditionLoop(this::isDatabaseAdminReady);
  }

  private boolean conditionLoop(BooleanSupplier condition) {
    for (int i = 0; i < config.getMaxReadyAttempts(); i++) {
      try {
        if (condition.getAsBoolean()) {
          return true;
        }
        pause();
      } catch (CommandException e) {
        pause();
      }
    }
    return false;
  }

  private void pause() {
    try {
      Thread.sleep(conditionPauseMillis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Check connectivity via trying to make a JDBC connection.
   */
  boolean checkConnectivity() {
    return checkConnectivity(checkConnectivityUsingAdmin);
  }

  /**
   * Check connectivity using admin user or dbUser.
   */
  boolean checkConnectivity(boolean useAdmin) {
    try {
      log.log(Level.TRACE, "checkConnectivity on {0} ... ", config.containerName());
      try (Connection connection = useAdmin ? config.createAdminConnection() : config.createConnectionNoSchema()) {
        log.log(Level.DEBUG, "connectivity confirmed for {0}", config.containerName());
      }
      return true;
    } catch (Throwable e) {
      if (e.getMessage().contains("No suitable driver found for")) {
        throw new RuntimeException("Error checking connectivity, missing JDBC Driver? " + e.getMessage(), e);
      }
      log.log(Level.TRACE, "connection failed: " + e.getMessage());
      return false;
    }
  }

  boolean defined(String val) {
    return val != null && !val.trim().isEmpty();
  }

  void runDbSqlFile(String dbName, String dbUser, String sqlFile) {
    if (defined(sqlFile)) {
      File file = getResourceOrFile(sqlFile);
      if (file != null) {
        runSqlFile(file, dbUser, dbName);
      }
    }
  }

  void runSqlFile(File file, String dbUser, String dbName) {
    if (copyFileToContainer(file)) {
      String containerFilePath = "/tmp/" + file.getName();
      executeSqlFile(dbUser, dbName, containerFilePath);
    }
  }

  File getResourceOrFile(String sqlFile) {

    File file = new File(sqlFile);
    if (!file.exists()) {
      file = checkFileResource(sqlFile);
    }
    if (file == null) {
      log.log(Level.ERROR, "Could not find SQL file. No file exists at location or resource path for: " + sqlFile);
    }
    return file;
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
      log.log(Level.ERROR, "Failed to obtain File from resource for init SQL file: " + sqlFile, e);
    }
    // not found
    return null;
  }

  boolean copyFileToContainer(File sourceFile) {
    ProcessBuilder pb = copyFileToContainerProcess(sourceFile);
    return execute(pb, "Failed to copy file " + sourceFile.getAbsolutePath() + " to container");
  }

  private ProcessBuilder copyFileToContainerProcess(File sourceFile) {

    //docker cp /tmp/init-file.sql ut_postgres:/tmp/init-file.sql
    String dest = config.containerName() + ":/tmp/" + sourceFile.getName();

    List<String> args = new ArrayList<>();
    args.add(config.docker());
    args.add("cp");
    args.add(sourceFile.getAbsolutePath());
    args.add(dest);
    return createProcessBuilder(args);
  }

  /**
   * Execute looking for expected message in stdout with no error logging.
   */
  boolean execute(String expectedLine, ProcessBuilder pb) {
    return execute(expectedLine, pb, null);
  }

  /**
   * Execute looking for expected message in stdout.
   */
  boolean execute(String expectedLine, ProcessBuilder pb, String errorMessage) {
    List<String> outLines = ProcessHandler.process(pb).getOutLines();
    if (!stdoutContains(outLines, expectedLine)) {
      if (errorMessage != null) {
        log.log(Level.ERROR, errorMessage + " stdOut:" + outLines + " Expected message:" + expectedLine);
      }
      return false;
    }
    return true;
  }

  /**
   * Execute looking for expected message in stdout.
   */
  boolean executeWithout(String errorMatch, ProcessBuilder pb, String errorMessage) {
    List<String> outLines = ProcessHandler.process(pb).getOutLines();
    if (stdoutContains(outLines, errorMatch)) {
      log.log(Level.ERROR, errorMessage + " stdOut:" + outLines);
      return false;
    }
    return true;
  }

  /**
   * Return true if the stdout contains the expected text.
   */
  boolean stdoutContains(List<String> outLines, String expectedLine) {
    for (String outLine : outLines) {
      if (outLine.contains(expectedLine)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Execute expecting no output to stdout.
   */
  boolean execute(ProcessBuilder pb, String errorMessage) {
    List<String> outLines = ProcessHandler.process(pb).getOutLines();
    if (!outLines.isEmpty()) {
      log.log(Level.ERROR, errorMessage + " stdOut:" + outLines);
      return false;
    }
    return true;
  }

  void sqlProcess(Consumer<Connection> runner) {
    try (Connection connection = config.createAdminConnection()) {
      runner.accept(connection);
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to execute sql", e);
    }
  }

  void sqlRun(Connection connection, String sql) {
    log.log(Level.DEBUG, "sqlRun: {0}", sql);
    try (Statement statement = connection.createStatement()) {
      statement.execute(sql);
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to execute sql [" + sql + "]", e);
    }
  }

  boolean sqlHasRow(Connection connection, String sql) {
    log.log(Level.TRACE, "sqlRun: {0}", sql);
    try (Statement statement = connection.createStatement()) {
      try (ResultSet resultSet = statement.executeQuery(sql)) {
        if (resultSet.next()) {
          return true;
        }
      }
      return false;
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to execute sql", e);
    }
  }

  boolean sqlQueryMatch(Connection connection, String sql, String match) throws SQLException {
    log.log(Level.TRACE, "sqlRun: {0}", sql);
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      try (ResultSet resultSet = stmt.executeQuery()) {
        while (resultSet.next()) {
          if (resultSet.getString(1).equalsIgnoreCase(match)) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
