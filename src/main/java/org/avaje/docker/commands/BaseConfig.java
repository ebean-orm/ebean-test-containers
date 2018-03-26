package org.avaje.docker.commands;

import org.avaje.docker.container.ContainerConfig;

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
  protected String port;

  /**
   * The internal port.
   */
  protected String internalPort;

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

  BaseConfig(String platform, String port, String internalPort, String version) {
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

  /**
   * Load configuration from properties.
   */
  public BaseConfig setProperties(Properties properties) {
    if (properties == null) {
      return this;
    }
    docker = properties.getProperty("docker", docker);

    containerName = prop(properties,"containerName", containerName);
    image = prop(properties,"image", image);
    port = prop(properties,"port", port);
    internalPort = prop(properties,"internalPort", internalPort);

    startMode = properties.getProperty("startMode", startMode);
    startMode = prop(properties,"startMode", startMode);

    stopMode = properties.getProperty("stopMode", stopMode);
    stopMode = prop(properties,"stopMode", stopMode);

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
    return properties.getProperty(platform+"."+key, defaultValue);
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
  public BaseConfig setPort(String port) {
    this.port = port;
    return this;
  }

  /**
   * Set the internal (to the container) port.
   */
  public BaseConfig setInternalPort(String internalPort) {
    this.internalPort = internalPort;
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

  public int getMaxReadyAttempts() {
    return maxReadyAttempts;
  }

  public String getDocker() {
    return docker;
  }

}
