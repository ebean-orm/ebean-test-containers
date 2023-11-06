package io.ebean.test.containers;

import java.lang.System.Logger.Level;

/**
 * Common DB Container.
 */
abstract class BaseDbContainer<C extends BaseDbContainer<C>> extends DbContainer<C> {

  protected static final System.Logger log = Commands.log;

  BaseDbContainer(DbConfig config) {
    super(config);
  }

  /**
   * Create the database, schema and user via docker commands.
   */
  protected abstract void createDbPreConnectivity();

  /**
   * Drop and create the database, schema and user via docker commands.
   */
  protected abstract void dropCreateDbPreConnectivity();

  /**
   * Create database, schema and user via JDBC .
   */
  protected void createDbPostConnectivity() {
    // do nothing by default
  }

  /**
   * Drop and create database, schema and user via JDBC .
   */
  protected void dropCreateDbPostConnectivity() {
    // do nothing by default
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
    if (startIfNeeded() && fastStart()) {
      // container was running, fast start enabled and passed
      // so skip the usual checks for user, extensions and connectivity
      createDbPostConnectivity();
      return true;
    }
    if (!waitForDatabaseReady()) {
      log.log(Level.WARNING, "Failed waitForDatabaseReady for container {0}", config.containerName());
      return false;
    }
    createDbPreConnectivity();
    if (!waitForConnectivity()) {
      log.log(Level.WARNING, "Failed waiting for connectivity");
      return false;
    }
    createDbPostConnectivity();
    return true;
  }

  /**
   * Start with a drop and create of the database and user.
   */
  @Override
  public boolean startWithDropCreate() {
    startIfNeeded();
    if (!waitForDatabaseReady()) {
      log.log(Level.WARNING, "Failed waitForDatabaseReady for container {0}", config.containerName());
      return false;
    }

    dropCreateDbPreConnectivity();
    if (!waitForConnectivity()) {
      log.log(Level.WARNING, "Failed waiting for connectivity");
      return false;
    }
    dropCreateDbPostConnectivity();
    return true;
  }

}
