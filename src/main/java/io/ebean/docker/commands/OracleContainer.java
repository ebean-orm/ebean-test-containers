package io.ebean.docker.commands;

import io.ebean.docker.commands.process.ProcessHandler;
import io.ebean.docker.container.Container;
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
   * Create with configuration.
   */
  public OracleContainer(OracleConfig config) {
    super(config);
    this.oracleConfig = config;
    this.checkConnectivityUingAdmin = true;
  }

  @Override
  protected ProcessBuilder runProcess() {

    List<String> args = dockerRun();
    args.add("-p");
    args.add(oracleConfig.getApexPort() + ":" + oracleConfig.getInternalApexPort());
    args.add(config.getImage());
    return createProcessBuilder(args);
  }

  @Override
  protected boolean isDatabaseAdminReady() {
    return checkConnectivity(true);
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
    if (!checkConnectivity(true)) {
      log.error("Ran out of time waiting for Oracle Database ready - probably not started.  Check via:  docker logs -f ut_oracle");
    }
  }

  @Override
  protected boolean isDatabaseReady() {
    return logsContain("Starting Oracle Database", null);
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

    log.info("Drop and create database user {}", dbConfig.getUsername());
    sqlProcess(connection -> {
      if (userExists(connection)) {
        runSql(connection, "drop user " + dbConfig.getUsername() + " cascade");
      }
      runSql(connection, "create user " + dbConfig.getUsername() + " identified by " + dbConfig.getPassword());
      runSql(connection, "grant connect, resource,  create view, unlimited tablespace to " + dbConfig.getUsername());
    });
    return true;
  }


  /**
   * Create the database user.
   */
  public boolean createUserIfNeeded() {
    log.info("Create database user {} if not exists", dbConfig.getUsername());
    sqlProcess(connection -> {
      if (!userExists(connection)) {
        runSql(connection, "create user " + dbConfig.getUsername() + " identified by " + dbConfig.getPassword());
        runSql(connection, "grant connect, resource, create view, unlimited tablespace to " + dbConfig.getUsername());
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
      statement.setString(1, dbConfig.getUsername().toLowerCase());
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
