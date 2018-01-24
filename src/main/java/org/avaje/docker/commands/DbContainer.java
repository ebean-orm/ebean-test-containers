package org.avaje.docker.commands;

import org.avaje.docker.commands.process.ProcessHandler;
import org.avaje.docker.container.Container;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

abstract class DbContainer extends BaseContainer implements Container {

  final DbConfig dbConfig;

  DbContainer(DbConfig config) {
    super(config);
    this.dbConfig = config;
  }

  @Override
  void logStarted() {
    log.info("Started container {} with port:{} dbName:{} dbUser:{}", config.containerName(), config.getPort(), dbConfig.getDbName(), dbConfig.getDbUser());
  }

  @Override
  public boolean start() {
    return logStart(startForMode());
  }

  /**
   * Start with a mode of 'create', 'dropCreate' or 'container'.
   * <p>
   * Expected that mode create will be best most of the time.
   */
  protected boolean startForMode() {
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
   * Start the container only doing nothing to ensure the DB or user exist.
   */
  public boolean startContainerOnly() {
    return startWithConnectivity();
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
   * Return true when the DB is ready for taking commands (like create database, user etc).
   */
  public boolean waitForDatabaseReady() {
    try {
      for (int i = 0; i < config.getMaxReadyAttempts(); i++) {
        if (isDatabaseReady()) {
          return isDatabaseAdminReady();
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
   * Additionally check that the DB admin user can connection (sql server).
   */
  protected boolean isDatabaseAdminReady() {
    // do nothing by default
    return true;
  }

  boolean checkConnectivity() {
    try {
      log.debug("checkConnectivity ... ");
      Connection connection = config.createConnection();
      connection.close();
      log.debug("connectivity confirmed ");
      return true;

    } catch (SQLException e) {
      log.trace("connection failed: " + e.getMessage());
      return false;
    }
  }

  boolean defined(String val) {
    return val != null && !val.trim().isEmpty();
  }

  /**
   * Execute looking for expected message in stdout.
   */
  boolean execute(String expectedLine, ProcessBuilder pb, String errorMessage) {
    List<String> outLines = ProcessHandler.process(pb).getStdOutLines();
    if (!stdoutContains(outLines, expectedLine)) {
      log.error(errorMessage + " stdOut:" + outLines + " Expected message:" + expectedLine);
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
    List<String> outLines = ProcessHandler.process(pb).getStdOutLines();
    if (!outLines.isEmpty()) {
      log.error(errorMessage + " stdOut:" + outLines);
      return false;
    }
    return true;
  }

}
