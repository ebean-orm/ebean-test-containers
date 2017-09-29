package org.avaje.docker.commands;

import org.avaje.docker.container.ContainerConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Configuration for an DBMS like Postgres, MySql, Oracle, SQLServer
 */
public abstract class DbConfig implements ContainerConfig {

  /**
   * The database platform.
   * <p>
   * Expected to be one of 'postgres','mysql', 'oracle' or 'sqlserver'.
   */
  private final String platform;

  /**
   * Container name.
   */
  private String containerName;

  /**
   * The exposed port.
   */
  private String port;

  /**
   * The internal port.
   */
  private String internalPort;

  /**
   * Image name.
   */
  private String image;

  /**
   * The mode used when starting (create, dropCreate, container [only]).
   */
  private String startMode = "create";

  /**
   * The mode used when stopping (stop, remove).
   */
  private String stopMode = "remove";

  /**
   * Set for in-memory tmpfs use.
   */
  private String tmpfs;// = "/var/lib/postgresql/data:rw";

  /**
   * Database admin password.
   */
  private String dbAdminPassword = "admin";

  /**
   * Database name to use.
   */
  private String dbName = "test_db";

  /**
   * Database user to use.
   */
  private String dbUser = "test_user";

  /**
   * Database password for the user.
   */
  private String dbPassword = "test";

  /**
   * Comma delimited list of database extensions required (hstore, pgcrypto etc).
   */
  private String dbExtensions;

  /**
   * Maximum number of attempts to find the 'database ready to accept connections' log message in the container.
   * <p>
   * 50 attempts equates to 5 seconds.
   * </p>
   */
  private int maxReadyAttempts = 50;

  /**
   * Set to true to run in-memory mode.
   */
  private boolean inMemory;

  /**
   * Docker command.
   */
  public String docker = "docker";

  DbConfig(String platform, String port, String internalPort, String version) {
    this.platform = platform;
    this.port = port;
    this.internalPort = internalPort;
    this.containerName = "ut_" + platform;
    this.image = platform + ":" + version;
  }

  /**
   * Return a description of the configuration.
   */
  @Override
  public String startDescription() {
    return "starting " + platform + " container:" + containerName + " port:" + port + " db:" + dbName + " user:" + dbUser + " extensions:" + dbExtensions + " startMode:" + startMode;
  }

  @Override
  public String stopDescription() {
    return "stopping " + platform + " container:" + containerName + " stopMode:" + stopMode;
  }

  @Override
  public String platform() {
    return platform;
  }

  @Override
  public String containerName() {
    return containerName;
  }

  @Override
  public void setStartMode(String startMode) {
    this.startMode = startMode;
  }

  @Override
  public void setStopMode(String stopMode) {
    this.stopMode = stopMode;
  }

  /**
   * Return a Connection to the database (make sure you close it).
   */
  @Override
  public Connection createConnection() throws SQLException {
    return DriverManager.getConnection(jdbcUrl(), getDbUser(), getDbPassword());
  }

  /**
   * Load configuration from properties.
   */
  public DbConfig withProperties(Properties properties) {
    if (properties == null) {
      return this;
    }
    docker = properties.getProperty("docker", docker);

    containerName = prop(properties,"containerName", containerName);
    image = prop(properties,"image", image);
    port = prop(properties,"port", port);
    internalPort = prop(properties,"internalPort", internalPort);

    startMode = prop(properties,"startMode", startMode);
    stopMode = prop(properties,"stopMode", stopMode);
    inMemory = Boolean.parseBoolean(prop(properties,"inMemory", Boolean.toString(inMemory)));

    tmpfs = prop(properties,"tmpfs", tmpfs);
    dbName = prop(properties, "dbName", dbName);
    dbUser = prop(properties, "dbUser", dbUser);
    dbPassword = prop(properties,"dbPassword", dbPassword);
    dbExtensions = prop(properties,"dbExtensions", dbExtensions);
    dbAdminPassword = prop(properties, "dbAdminPassword", dbAdminPassword);

    String maxVal = prop(properties, "maxReadyAttempts", null);
    if (maxVal != null) {
      try {
        this.maxReadyAttempts = Integer.parseInt(maxVal);
      } catch (NumberFormatException e) {
        // ignore error
      }
    }
    return this;
  }

  String prop(Properties properties, String key, String defaultValue) {
    return properties.getProperty(platform+"."+key, defaultValue);
  }


  /**
   * Set the container name.
   */
  public DbConfig withContainerName(String containerName) {
    this.containerName = containerName;
    return this;
  }

  /**
   * Set the exposed port.
   */
  public DbConfig withPort(String port) {
    this.port = port;
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
  public DbConfig withAdminPassword(String adminPassword) {
    this.dbAdminPassword = adminPassword;
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
  public DbConfig withUser(String user) {
    this.dbUser = user;
    return this;
  }

  /**
   * Set the DB password.
   */
  public DbConfig withPassword(String password) {
    this.dbPassword = password;
    return this;
  }

  /**
   * Set the DB extensions to install (Postgres hstore, pgcrypto etc)
   */
  public DbConfig withExtensions(String extensions) {
    this.dbExtensions = extensions;
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

  public boolean isInMemory() {
    return inMemory;
  }

  public String getPort() {
    return port;
  }

  public String getInternalPort() {
    return internalPort;
  }

  public String getImage() {
    return image;
  }

  public String getStartMode() {
    return startMode;
  }

  public String getStopMode() {
    return stopMode;
  }

  public String getTmpfs() {
    return tmpfs;
  }

  public String getDbAdminPassword() {
    return dbAdminPassword;
  }

  public String getDbName() {
    return dbName;
  }

  public String getDbUser() {
    return dbUser;
  }

  public String getDbPassword() {
    return dbPassword;
  }

  public String getDbExtensions() {
    return dbExtensions;
  }

  public int getMaxReadyAttempts() {
    return maxReadyAttempts;
  }

  public String getDocker() {
    return docker;
  }

}
