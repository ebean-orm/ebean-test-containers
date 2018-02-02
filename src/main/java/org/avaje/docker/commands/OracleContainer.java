package org.avaje.docker.commands;

import org.avaje.docker.commands.process.ProcessHandler;
import org.avaje.docker.container.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Commands for controlling an Oracle docker container.
 */
public class OracleContainer extends DbContainer implements Container {

  /**
   * Create Postgres container with configuration from properties.
   */
  public static OracleContainer create(String version, Properties properties) {
    return new OracleContainer(new OracleConfig(version, properties));
  }

  private static final Logger log = LoggerFactory.getLogger(Commands.class);

  private final OracleConfig oracleConfig;

  /**
   * Position is logs that we are tailing.
   */
  private int logPosition;

  /**
   * Create with configuration.
   */
  public OracleContainer(OracleConfig config) {
    super(config);
    this.oracleConfig = config;
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
    args.add("-p");
    args.add(oracleConfig.getApexPort() + ":" + oracleConfig.getInternalApexPort());

    args.add(config.getImage());
    return createProcessBuilder(args);
  }

  @Override
  boolean checkConnectivity() {
    return checkConnectivity(true);
  }

  @Override
  void runContainer() {
    log.info("Starting Oracle container, this will take some time ...");
    ProcessHandler.process(runProcess());
    waitForOracle();
  }

  /**
   * Oracle starting up from scratch so this will likely take minutes.
   * Tail the logs looking for Database Ready message.
   */
  private void waitForOracle() {

    int totalWaitCount = oracleConfig.getStartupWaitMinutes() * 10 * 60;
    for (int i = 0; i < totalWaitCount; i++) {
      if (isOracleReadyViaContainerLogs()) {
        return;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("Interrupted - oracle probably not started");
      }
    }
    log.error("Ran out of time waiting for Oracle Database ready - probably not started.  Check via:  docker logs -f ut_oracle");
  }

  /**
   * Tail the logs looking for Database ready message.
   */
  private boolean isOracleReadyViaContainerLogs() {

    List<String> currentLogs = logs();
    if (!currentLogs.isEmpty()) {
      List<String> extraLogs = currentLogs.subList(logPosition, currentLogs.size());
      for (String extraLog : extraLogs) {
        log.info("oracle container> " + extraLog);
        if (extraLog.contains("Database ready to use.")) {
          return true;
        }
      }
      logPosition = currentLogs.size();
    }
    return false;
  }

  @Override
  protected boolean isDatabaseReady() {
    return logsContain("Database ready to use.");
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
    if (!waitForConnectivity()) {
      log.warn("Failed waiting for connectivity");
      return false;
    }
    return createUserIfNeeded();
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
    if (!waitForConnectivity()) {
      log.warn("Failed waiting for connectivity");
      return false;
    }
    return dropCreateUser();
  }

  private boolean dropCreateUser() {

    log.info("Drop and create database user {}", dbConfig.getDbUser());
    sqlProcess(connection -> {
      if (userExists(connection)) {
        runSql(connection, "drop user " + dbConfig.getDbUser() + " cascade");
      }
      runSql(connection, "create user " + dbConfig.getDbUser() + " identified by " + dbConfig.getDbPassword());
      runSql(connection, "grant create view, connect, resource, unlimited tablespace to " + dbConfig.getDbUser());
    });
    return true;
  }


  /**
   * Create the database user.
   */
  public boolean createUserIfNeeded() {
    log.info("Create database user {} if not exists", dbConfig.getDbUser());
    sqlProcess(connection -> {
      if (!userExists(connection)) {
        runSql(connection, "create user " + dbConfig.getDbUser() + " identified by " + dbConfig.getDbPassword());
        runSql(connection, "grant connect, resource, unlimited tablespace to " + dbConfig.getDbUser());
      }
    });
    return true;
  }

  private boolean userExists(Connection connection) {
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    try {
      String sql = "select count(*) from dba_users where lower(username) = ?";
      log.debug("execute: " + sql);
      statement = connection.prepareStatement(sql);
      statement.setString(1, dbConfig.getDbUser().toLowerCase());
      resultSet = statement.executeQuery();

      if (resultSet.next()) {
        int count = resultSet.getInt(1);
        return count == 1;
      }

      return false;

    } catch (SQLException e) {
      throw new IllegalStateException("Failed to execute sql to check if user exists", e);

    } finally {
      close(resultSet);
      close(statement);
    }
  }


  private boolean sqlProcess(Consumer<Connection> runner) {

    Connection connection = null;
    try {
      connection = config.createAdminConnection();
      runner.accept(connection);
      return true;

    } catch (SQLException e) {
      throw new IllegalStateException("Failed to execute sql", e);

    } finally {
      close(connection);
    }
  }

  private void runSql(Connection connection, String sql) {
    Statement statement = null;
    try {
      log.debug("execute: " + sql);
      statement = connection.createStatement();
      statement.execute(sql);

    } catch (SQLException e) {
      throw new IllegalStateException("Failed to execute sql", e);

    } finally {
      close(statement);
    }
  }

  private void close(ResultSet resultSet) {
    if (resultSet != null) {
      try {
        resultSet.close();
      } catch (SQLException e) {
        log.warn("Error closing resultSet", e);
      }
    }
  }

  private void close(Statement statement) {
    if (statement != null) {
      try {
        statement.close();
      } catch (SQLException e) {
        log.warn("Error closing statement", e);
      }
    }
  }

  private void close(Connection connection) {
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException e) {
        log.warn("Error closing connection", e);
      }
    }
  }

}
