package org.avaje.docker.commands;

import org.avaje.docker.commands.process.ProcessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

abstract class BaseDbCommands implements DbCommands {

  static final Logger log = LoggerFactory.getLogger(Commands.class);

  final DbConfig config;

  final Commands commands;

  BaseDbCommands(DbConfig config) {
    this.config = config;
    this.commands = new Commands(config.docker);
  }

  /**
   * Return the JDBC connection URL used for testing connectivity.
   */
  protected abstract String jdbcUrl();

  /**
   * Return the ProcessBuilder used to execute the container run command.
   */
  protected abstract ProcessBuilder runProcess();

  @Override
  public String getStartDescription() {
    return config.getStartDescription();
  }

  @Override
  public String getStopDescription() {
    return config.getStopDescription();
  }

  /**
   * Start the container checking if it is already running.
   */
  void startIfNeeded() {

    if (!commands.isRunning(config.name)) {
      if (commands.isRegistered(config.name)) {
        commands.start(config.name);

      } else {
        log.debug("run postgres container {}", config.name);
        runContainer();
      }
    }
  }

  void runContainer() {
    ProcessHandler.process(runProcess());
  }

  /**
   * Return a Connection to the database (make sure you close it).
   */
  public Connection createConnection() throws SQLException {
    return DriverManager.getConnection(jdbcUrl(), config.dbUser, config.dbPassword);
  }

  boolean checkConnectivity() {
    try {
      log.debug("checkConnectivity ... ");
      Connection connection = createConnection();
      connection.close();
      log.debug("connectivity confirmed ");
      return true;

    } catch (SQLException e) {
      log.trace("connection failed: " + e.getMessage());
      return false;
    }
  }

  /**
   * Return true when we can make IP connections to the database (JDBC).
   */
  boolean waitForConnectivity() {
    for (int i = 0; i < 120; i++) {
      if (checkConnectivity()) {
        return true;
      }
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return false;
  }

  /**
   * Stop using the configured stopMode of 'stop' or 'remove'.
   * <p>
   * Remove additionally removes the container (expected use in build agents).
   */
  public void stop() {
    String mode = config.dbStopMode.toLowerCase().trim();
    switch (mode) {
      case "stop":
        stop();
        break;
      case "remove":
        stopContainerRemove();
        break;
      default:
        stopContainer();
    }
  }

  /**
   * Stop and remove the container effectively deleting the database.
   */
  public void stopContainerRemove() {
    commands.stopRemove(config.name);
  }

  /**
   * Stop the postgres container.
   */
  public void stopContainer() {
    commands.stopIfRunning(config.name);
  }


  boolean userDefined() {
    return defined(config.dbUser);
  }

  boolean databaseDefined() {
    return defined(config.dbName);
  }

  boolean defined(String val) {
    return val != null && !val.trim().isEmpty();
  }

  ProcessBuilder createProcessBuilder(List<String> args) {
    ProcessBuilder pb = new ProcessBuilder();
    pb.command(args);
    return pb;
  }
}
