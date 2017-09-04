package org.avaje.docker.commands;

import java.util.Properties;

/**
 * Configuration for an DBMS like Postgres, MySql, Oracle, SQLServer
 */
public class DbConfig {

  /**
   * The database platform.
   * <p>
   * Expected to be one of 'postgres','mysql', 'oracle' or 'sqlserver'.
   */
  public String platform;

  /**
   * Container name.
   */
  public String name = "ut_postgres";

  /**
   * The mode used when starting (create, dropCreate, container [only]).
   */
  public String dbStartMode = "create";

  /**
   * The mode used when stopping (stop, remove).
   */
  public String dbStopMode = "remove";

  /**
   * The exposed port.
   */
  public String dbPort = "6432";

  /**
   * The internal port.
   */
  public String internalPort = "5432";

  /**
   * Set for in-memory tmpfs use.
   */
  public String tmpfs = "/var/lib/postgresql/data:rw";

  /**
   * Image name.
   */
  public String image = "postgres:9.5.4";

  /**
   * Database admin password.
   */
  public String dbAdminPassword = "admin";

  /**
   * Database name to use.
   */
  public String dbName = "test_db";

  /**
   * Database user to use.
   */
  public String dbUser = "test_user";

  /**
   * Database password for the user.
   */
  public String dbPassword = "test";

  /**
   * Comma delimited list of database extensions required (hstore, pgcrypto etc).
   */
  public String dbExtensions;

  /**
   * Maximum number of attempts to find the 'database ready to accept connections' log message in the container.
   * <p>
   * 50 attempts equates to 5 seconds.
   * </p>
   */
  public int maxReadyAttempts = 50;

  /**
   * Docker command.
   */
  public String docker = "docker";

  /**
   * Return true if a db platform has been defined (Postgres, MySql etc).
   */
  public boolean hasPlatform() {
    return platform != null;
  }

  /**
   * Return a description of the configuration.
   */
  public String getStartDescription() {
    return "starting " + platform + " container:" + name + " port:" + dbPort + " db:" + dbName + " user:" + dbUser + " extensions:" + dbExtensions + " startMode:" + dbStartMode;
  }

  public String getStopDescription() {
    return "stopping " + platform + " container:" + name + " stopMode:" + dbStopMode;
  }

  /**
   * Load configuration from properties.
   */
  public DbConfig withProperties(Properties properties) {
    if (properties == null) {
      return this;
    }
    platform = properties.getProperty("dbPlatform", platform);
    docker = properties.getProperty("docker", docker);
    name = properties.getProperty("dbContainerName", name);
    dbPort = properties.getProperty("dbPort", dbPort);
    internalPort = properties.getProperty("dbInternalPort", internalPort);
    dbAdminPassword = properties.getProperty("dbAdminPassword", dbAdminPassword);
    tmpfs = properties.getProperty("dbTmpfs", tmpfs);
    image = properties.getProperty("dbImage", image);


    dbStartMode = properties.getProperty("dbStartMode", dbStartMode);
    dbStopMode = properties.getProperty("dbStopMode", dbStopMode);
    dbName = properties.getProperty("dbName", dbName);
    dbUser = properties.getProperty("dbUser", dbUser);
    dbPassword = properties.getProperty("dbPassword", dbPassword);
    dbExtensions = properties.getProperty("dbExtensions", dbExtensions);
    dbExtensions = properties.getProperty("dbExtensions", dbExtensions);

    String maxVal = properties.getProperty("dbMaxReadyAttempts");
    if (maxVal != null) {
      try {
        this.maxReadyAttempts = Integer.parseInt(maxVal);
      } catch (NumberFormatException e) {
        // ignore error
      }
    }
    return this;
  }

  /**
   * Set the container name.
   */
  public DbConfig withName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Set the exposed port.
   */
  public DbConfig withDbPort(String dbPort) {
    this.dbPort = dbPort;
    return this;
  }

  /**
   * Set the internal (to the container) port.
   */
  public DbConfig withInternalPort(String internalPort) {
    this.internalPort = internalPort;
    return this;
  }

  /**
   * Set the password for the DB admin user.
   */
  public DbConfig withDbAdminPassword(String dbAdminPassword) {
    this.dbAdminPassword = dbAdminPassword;
    return this;
  }

  /**
   * Set the temp fs for in-memory use.
   */
  public DbConfig withTmpfs(String tmpfs) {
    this.tmpfs = tmpfs;
    return this;
  }

  /**
   * Set the docker image to use.
   */
  public DbConfig withImage(String image) {
    this.image = image;
    return this;
  }

  /**
   * Set the DB name.
   */
  public DbConfig withDbName(String dbName) {
    this.dbName = dbName;
    return this;
  }

  /**
   * Set the DB user.
   */
  public DbConfig withDbUser(String dbUser) {
    this.dbUser = dbUser;
    return this;
  }

  /**
   * Set the DB password.
   */
  public DbConfig withDbPassword(String dbPassword) {
    this.dbPassword = dbPassword;
    return this;
  }

  /**
   * Set the DB extensions to install (Postgres hstore, pgcrypto etc)
   */
  public DbConfig withDbExtensions(String dbExtensions) {
    this.dbExtensions = dbExtensions;
    return this;
  }

  /**
   * Set the max attempts to wait for DB ready.
   */
  public DbConfig withMaxReadyAttempts(int maxReadyAttempts) {
    this.maxReadyAttempts = maxReadyAttempts;
    return this;
  }

  /**
   * Set the docker command to use (defaults to 'docker').
   */
  public DbConfig withDocker(String docker) {
    this.docker = docker;
    return this;
  }
}
