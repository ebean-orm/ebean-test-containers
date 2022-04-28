package io.ebean.docker.commands;

import java.sql.Connection;

abstract class JdbcBaseDbContainer extends DbContainer {

  JdbcBaseDbContainer(DbConfig<?> config) {
    super(config);
    this.checkConnectivityUsingAdmin = true;
  }

  abstract void createDatabase();

  abstract void dropCreateDatabase();

  @Override
  public boolean startWithCreate() {
    if (!startContainerWithWait()) {
      return false;
    }
    createDatabase();
    return true;
  }

  @Override
  public boolean startWithDropCreate() {
    if (!startContainerWithWait()) {
      return false;
    }
    dropCreateDatabase();
    return true;
  }

  private boolean startContainerWithWait() {
    if (checkAlreadyRunning()) {
      return true;
    }
    startIfNeeded();
    if (!waitForConnectivity()) {
      log.warn("Failed waiting for connectivity");
      return false;
    }
    return true;
  }

  protected boolean checkAlreadyRunning() {
    try (Connection connection = dbConfig.createAdminConnection()) {
      return true;
    } catch (Throwable e) {
      // no connectivity
      return false;
    }
  }

  @Override
  public boolean waitForDatabaseReady() {
    // Just rely on waitForConnectivity() instead
    return true;
  }

  @Override
  protected boolean isDatabaseReady() {
    // Just rely on waitForConnectivity() instead
    return true;
  }

  @Override
  protected boolean isDatabaseAdminReady() {
    // Just rely on waitForConnectivity() instead
    return true;
  }
}
