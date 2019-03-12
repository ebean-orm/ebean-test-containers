package io.ebean.docker.commands;

import io.ebean.docker.container.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common DB Container.
 */
public abstract class BaseDbContainer extends DbContainer implements Container {

  protected static final Logger log = LoggerFactory.getLogger(Commands.class);

  BaseDbContainer(DbConfig config) {
    super(config);
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
    if (startIfNeeded() && fastStart()) {
      // container was running, fast start enabled and passed
      // so skip the usual checks for user, extensions and connectivity
      return true;
    }
    if (!waitForDatabaseReady()) {
      log.warn("Failed waitForDatabaseReady for container {}", config.containerName());
      return false;
    }
    createUser(true);
    createDatabase(true);
    createDatabaseExtensions();
    if (!waitForConnectivity()) {
      log.warn("Failed waiting for connectivity");
      return false;
    }
    return true;
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

    if (!dropDatabaseIfExists() || !dropUserIfExists()) {
      // failed to drop existing db or user
      return false;
    }
    if (!createUser(false) || !createDatabase(false)) {
      // failed to create the db or user
      return false;
    }
    createDatabaseExtensions();
    if (!waitForConnectivity()) {
      log.warn("Failed waiting for connectivity");
      return false;
    }
    return true;
  }

  @Override
  protected boolean isFastStartDatabaseExists() {
    return databaseExists(dbConfig.getDbName());
  }

  /**
   * Create the database user.
   */
  public boolean createUser(boolean checkExists) {
    String extraDbUser = getExtraDbUser();
    if (defined(extraDbUser) && (!checkExists || !userExists(extraDbUser))) {
      if (!createUser(extraDbUser, getWithDefault(dbConfig.getExtraDbPassword(), dbConfig.getPassword()))) {
        log.error("Failed to create extra database user " + extraDbUser);
      }
    }
    if (checkExists && userExists(dbConfig.getUsername())) {
      return true;
    }
    return createUser(dbConfig.getUsername(), dbConfig.getPassword());
  }

  /**
   * Maybe return an extra user to create.
   * <p>
   * The extra user will default to be the same as the extraDB if that is defined.
   * Additionally we don't create an extra user IF it is the same as the main db user.
   */
  private String getExtraDbUser() {
    String extraUser = getWithDefault(dbConfig.getExtraDbUser(), dbConfig.getExtraDb());
    return extraUser != null && !extraUser.equals(dbConfig.getUsername()) ? extraUser : null;
  }

  /**
   * Create the database with the option of checking if if already exists.
   *
   * @param checkExists When true check the database doesn't already exists
   */
  public boolean createDatabase(boolean checkExists) {
    String extraDb = dbConfig.getExtraDb();
    if (defined(extraDb) && (!checkExists || !databaseExists(extraDb))) {
      String extraUser = getWithDefault(getExtraDbUser(), dbConfig.getUsername());
      if (!createDatabase(extraDb, extraUser, dbConfig.getExtraDbInitSqlFile(), dbConfig.getExtraDbSeedSqlFile())) {
        log.error("Failed to create extra database " + extraDb);
      }
    }
    if (checkExists && databaseExists(dbConfig.getDbName())) {
      return true;
    }
    return createDatabase(dbConfig.getDbName(), dbConfig.getUsername(), dbConfig.getInitSqlFile(), dbConfig.getSeedSqlFile());
  }

  private String getWithDefault(String value, String defaultValue) {
    return value == null ? defaultValue : value;
  }

  /**
   * Create the database extensions if defined.
   */
  public void createDatabaseExtensions() {

    String dbExtn = dbConfig.getExtensions();
    if (defined(dbExtn)) {
      if (defined(dbConfig.getExtraDb())) {
        createDatabaseExtensionsFor(dbExtn, dbConfig.getExtraDb());
      }
      createDatabaseExtensionsFor(dbExtn, dbConfig.getDbName());
    }
  }

  protected abstract void createDatabaseExtensionsFor(String dbExtn, String dbName);

  /**
   * Drop the database if it exists.
   */
  public boolean dropDatabaseIfExists() {
    String extraDb = dbConfig.getExtraDb();
    if (defined(extraDb) && !dropDatabaseIfExists(extraDb)) {
      log.error("Failed to drop extra database " + extraDb);
    }
    return dropDatabaseIfExists(dbConfig.getDbName());
  }

  private boolean dropDatabaseIfExists(String dbName) {
    if (databaseExists(dbName)) {
      return dropDatabase(dbName);
    }
    return true;
  }

  /**
   * Drop the database user if it exists.
   */
  public boolean dropUserIfExists() {
    String extraDbUser = getExtraDbUser();
    if (defined(extraDbUser) && !dropUserIfExists(extraDbUser)) {
      log.error("Failed to drop extra database user " + extraDbUser);
    }
    return dropUserIfExists(dbConfig.getUsername());
  }

  private boolean dropUserIfExists(String dbUser) {
    if (!userExists(dbUser)) {
      return true;
    }
    return dropUser(dbUser);
  }

  @Override
  protected abstract boolean isDatabaseAdminReady();

  /**
   * Return true if the database exists.
   */
  public abstract boolean databaseExists(String dbName);

  /**
   * Return true if the database user exists.
   */
  public abstract boolean userExists(String dbUser);

  protected abstract boolean createUser(String user, String pwd);

  protected abstract boolean createDatabase(String dbName, String dbUser, String initSqlFile, String seedSqlFile);

  @Override
  protected abstract void executeSqlFile(String dbUser, String dbName, String containerFilePath);

  protected abstract boolean dropDatabase(String dbName);

  protected abstract boolean dropUser(String dbUser);

  /**
   * Return True when we detect the database is ready (to create user and database etc).
   */
  public abstract boolean isDatabaseReady();

}
