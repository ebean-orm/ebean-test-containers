package org.avaje.docker.commands;

import org.avaje.docker.commands.process.ProcessHandler;
import org.avaje.docker.container.Container;
import org.avaje.docker.container.ContainerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

abstract class BaseContainer implements Container {

  protected static final Logger log = LoggerFactory.getLogger(Commands.class);

  protected final BaseConfig config;

  protected final Commands commands;

  BaseContainer(BaseConfig config) {
    this.config = config;
    this.commands = new Commands(config.docker);
  }

  /**
   * Return the ProcessBuilder used to execute the container run command.
   */
  protected abstract ProcessBuilder runProcess();

  @Override
  public ContainerConfig config() {
    return config;
  }

  @Override
  public boolean start() {
    startIfNeeded();
    if (!waitForConnectivity()) {
      log.warn("Failed waiting for connectivity");
      return false;
    }
    return true;
  }

  /**
   * Start the container checking if it is already running.
   */
  void startIfNeeded() {

    if (!commands.isRunning(config.containerName())) {
      if (commands.isRegistered(config.containerName())) {
        commands.start(config.containerName());

      } else {
        log.debug("run {} container {}", config.platform(), config.containerName());
        runContainer();
      }
    }
  }

  void runContainer() {
    ProcessHandler.process(runProcess());
  }

//  /**
//   * Return a Connection to the database (make sure you close it).
//   */
//  public Connection createConnection() throws SQLException {
//    return DriverManager.getConnection(config.jdbcUrl(), config.getDbUser(), config.getDbPassword());
//  }

//  boolean checkConnectivity() {
//    try {
//      log.debug("checkConnectivity ... ");
//      Connection connection = createConnection();
//      connection.close();
//      log.debug("connectivity confirmed ");
//      return true;
//
//    } catch (SQLException e) {
//      log.trace("connection failed: " + e.getMessage());
//      return false;
//    }
//  }

  abstract boolean checkConnectivity();

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
  @Override
  public void stop() {
    String mode = config.getStopMode().toLowerCase().trim();
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
    commands.stopRemove(config.containerName());
  }

  /**
   * Stop the postgres container.
   */
  public void stopContainer() {
    commands.stopIfRunning(config.containerName());
  }


//  boolean userDefined() {
//    return defined(config.getDbUser());
//  }
//
//  boolean databaseDefined() {
//    return defined(config.getDbName());
//  }

//  boolean defined(String val) {
//    return val != null && !val.trim().isEmpty();
//  }

  protected ProcessBuilder createProcessBuilder(List<String> args) {
    ProcessBuilder pb = new ProcessBuilder();
    pb.command(args);
    return pb;
  }
}
