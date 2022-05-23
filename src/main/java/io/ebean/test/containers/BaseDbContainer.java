package io.ebean.test.containers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common DB Container.
 */
abstract class BaseDbContainer extends DbContainer implements Container {

  protected static final Logger log = LoggerFactory.getLogger("io.ebean.test.containers");

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
      log.warn("Failed waitForDatabaseReady for container {}", config.containerName());
      return false;
    }
    createDbPreConnectivity();
    if (!waitForConnectivity()) {
      log.warn("Failed waiting for connectivity");
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
      log.warn("Failed waitForDatabaseReady for container {}", config.containerName());
      return false;
    }

    dropCreateDbPreConnectivity();
    if (!waitForConnectivity()) {
      log.warn("Failed waiting for connectivity");
      return false;
    }
    dropCreateDbPostConnectivity();
    return true;
  }

}
