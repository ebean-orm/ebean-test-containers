package io.ebean.test.containers;

import java.sql.Connection;

import static java.lang.System.Logger.Level.WARNING;

abstract class BaseJdbcContainer<C extends BaseJdbcContainer<C>> extends DbContainer<C> {

  BaseJdbcContainer(BaseDbBuilder<?, ?> config) {
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
      log.log(WARNING, "Failed waiting for connectivity");
      return false;
    }
    return true;
  }

  protected boolean checkAlreadyRunning() {
    if (dbConfig.randomPort()) {
      return false;
    }
    try (Connection connection = dbConfig.createAdminConnection()) {
      return connection.isValid(1);
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
