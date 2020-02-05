package io.ebean.docker.commands;

import io.ebean.docker.container.ContainerConfig;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Configuration for an DBMS like Postgres, MySql, Oracle, SQLServer
 */
public abstract class BaseConfig implements ContainerConfig {

  /**
   * The database platform.
   * <p>
   * Expected to be one of 'postgres','mysql', 'oracle' or 'sqlserver'.
   */
  protected final String platform;

  /**
   * Container name.
   */
  protected String containerName;

  /**
   * The exposed port.
   */
  protected int port;

  /**
   * The internal port.
   */
  protected int internalPort;

  /**
   * The exposed port.
   */
  protected int adminPort;

  /**
   * The internal port.
   */
  protected int adminInternalPort;

  /**
   * Image name.
   */
  protected String image;

  /**
   * The mode used when starting (create, dropCreate, container [only]).
   */
  protected String startMode = "create";

  /**
   * The mode used when stopping (stop, remove).
   */
  protected String stopMode = "stop";

  /**
   * Set when we want to automatically stop or remove the container via JVM shutdown hook.
   */
  protected String shutdownMode;

  /**
   * The character set to use.
   */
  protected String characterSet;

  /**
   * The collation to use.
   */
  protected String collation;

  /**
   * Maximum number of attempts to find the 'database ready to accept connections' log message in the container.
   * <p>
   * 100 attempts equates to 10 seconds.
   * </p>
   */
  protected int maxReadyAttempts = 300;

  /**
   * Docker command.
   */
  protected String docker = "docker";

  protected final String version;

  BaseConfig(String platform, int port, int internalPort, String version) {
    this.platform = platform;
    this.port = port;
    this.internalPort = internalPort;
    this.containerName = "ut_" + platform;
    this.image = platform + ":" + version;
    this.version = version;
  }

  /**
   * Return a description of the configuration.
   */
  @Override
  public String startDescription() {
    return "starting " + platform + " container:" + containerName + " port:" + port + " startMode:" + startMode;
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
  public String version() {
    return version;
  }

  @Override
  public void setStartMode(String startMode) {
    this.startMode = startMode;
  }

  @Override
  public void setStopMode(String stopMode) {
    this.stopMode = stopMode;
  }

  @Override
  public void setShutdownMode(String shutdownMode) {
    this.shutdownMode = shutdownMode;
  }

  /**
   * Return a Connection to the database (make sure you close it).
   */
  @Override
  public Connection createConnection() throws SQLException {
    throw new IllegalStateException("Not valid for this type");
  }

  @Override
  public Connection createConnectionNoSchema() throws SQLException {
    throw new IllegalStateException("Not valid for this type");
  }

  @Override
  public Connection createAdminConnection() throws SQLException {
    throw new IllegalStateException("Not valid for this type");
  }

  @Override
  public String jdbcUrl() {
    throw new IllegalStateException("Not valid for this type");
  }

  @Override
  public String jdbcAdminUrl() {
    return jdbcUrl();
  }

  /**
   * Load configuration from properties.
   */
  public BaseConfig setProperties(Properties properties) {
    if (properties == null) {
      return this;
    }
    docker = properties.getProperty("docker", docker);

    containerName = prop(properties, "containerName", containerName);
    image = prop(properties, "image", image);
    port = prop(properties, "port", port);
    internalPort = prop(properties, "internalPort", internalPort);
    adminPort = prop(properties, "adminPort", adminPort);
    adminInternalPort = prop(properties, "adminInternalPort", adminInternalPort);
    characterSet = prop(properties, "characterSet", characterSet);
    collation = prop(properties, "collation", collation);

    startMode = properties.getProperty("startMode", startMode);
    startMode = prop(properties, "startMode", startMode);

    stopMode = properties.getProperty("stopMode", stopMode);
    stopMode = prop(properties, "stopMode", stopMode);

    shutdownMode = properties.getProperty("shutdown", shutdownMode);
    shutdownMode = prop(properties, "shutdown", shutdownMode);

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

  protected String prop(Properties properties, String key, String defaultValue) {
    String val = properties.getProperty("ebean.test." + platform + "." + key, defaultValue);
    return properties.getProperty(platform + "." + key, val);
  }

  protected int prop(Properties properties, String key, int defaultValue) {
    String val = properties.getProperty("ebean.test." + platform + "." + key);
    val = properties.getProperty(platform + "." + key, val);
    return val == null ? defaultValue : Integer.parseInt(val);
  }

  /**
   * Set the container name.
   */
  public BaseConfig setContainerName(String containerName) {
    this.containerName = containerName;
    return this;
  }

  /**
   * Set the exposed port.
   */
  public BaseConfig setPort(int port) {
    this.port = port;
    return this;
  }

  /**
   * Set the internal (to the container) port.
   */
  public BaseConfig setInternalPort(int internalPort) {
    this.internalPort = internalPort;
    return this;
  }

  /**
   * Set the exposed admin port.
   */
  public BaseConfig setAdminPort(int adminPort) {
    this.adminPort = adminPort;
    return this;
  }

  /**
   * Set the internal admin (to the container) port.
   */
  public BaseConfig setAdminInternalPort(int adminInternalPort) {
    this.adminInternalPort = adminInternalPort;
    return this;
  }

  /**
   * Set the docker image to use.
   */
  public BaseConfig setImage(String image) {
    this.image = image;
    return this;
  }

  /**
   * Set the character set to use.
   */
  public BaseConfig setCharacterSet(String characterSet) {
    this.characterSet = characterSet;
    return this;
  }

  /**
   * Set the collation to use.
   */
  public BaseConfig setCollation(String collation) {
    this.collation = collation;
    return this;
  }

  /**
   * Set the max attempts to wait for DB ready.
   */
  public BaseConfig setMaxReadyAttempts(int maxReadyAttempts) {
    this.maxReadyAttempts = maxReadyAttempts;
    return this;
  }

  /**
   * Set the docker command to use (defaults to 'docker').
   */
  public BaseConfig setDocker(String docker) {
    this.docker = docker;
    return this;
  }

  public int getPort() {
    return port;
  }

  public int getInternalPort() {
    return internalPort;
  }

  public int getAdminPort() {
    return adminPort;
  }

  public int getAdminInternalPort() {
    return adminInternalPort;
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

  public int getMaxReadyAttempts() {
    return maxReadyAttempts;
  }

  public String getDocker() {
    return docker;
  }

  public String shutdownMode() {
    return shutdownMode;
  }

  public String getCharacterSet() {
    return characterSet;
  }

  public String getCollation() {
    return collation;
  }

  public boolean isExplicitCollation() {
    return collation != null || characterSet != null;
  }

  public boolean isDefaultCollation() {
    return "default".equals(collation);
  }

  public boolean isStopModeNone() {
    return "none".equals(stopMode);
  }

  /**
   * Clear the stopMode when detect already running.
   */
  public void clearStopMode() {
    this.stopMode = "none";
  }
}
