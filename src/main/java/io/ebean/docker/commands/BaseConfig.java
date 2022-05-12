package io.ebean.docker.commands;

import io.ebean.docker.container.ContainerBuilder;
import io.ebean.docker.container.StartMode;
import io.ebean.docker.container.StopMode;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Configuration for an DBMS like Postgres, MySql, Oracle, SQLServer
 */
abstract class BaseConfig<C,SELF extends BaseConfig<C,SELF>> implements ContainerBuilder<C,SELF> {

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
   * The host name. When running in Docker this is often set to <code>172.17.0.1</code.
   */
  protected String host = DockerHost.host();

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
  protected StartMode startMode = StartMode.Create;

  /**
   * By default, via JVM shutdown hook stop and remove the container unless there is a ~/.ebean/ignore-docker-shutdown marker file.
   */
  protected StopMode shutdownMode = StopMode.Auto;

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
    this.image = platform + ":" + version;
    this.version = version;
  }

  protected String docker() {
    return docker;
  }

  protected String getHost() {
    return host;
  }

  protected int getPort() {
    return port;
  }

  @SuppressWarnings("unchecked")
  protected SELF self() {
    return (SELF) this;
  }

  @Override
  public SELF startMode(StartMode startMode) {
    this.startMode = startMode;
    return self();
  }

  @Override
  public SELF shutdownMode(StopMode shutdownMode) {
    this.shutdownMode = shutdownMode;
    return self();
  }

  /**
   * Load configuration from properties.
   */
  @Override
  public SELF properties(Properties properties) {
    if (properties == null) {
      return self();
    }
    docker = properties.getProperty("docker", docker);
    containerName = prop(properties, "containerName", containerName);
    image = prop(properties, "image", image);
    host = prop(properties, "host", host);
    port = prop(properties, "port", port);
    internalPort = prop(properties, "internalPort", internalPort);
    adminPort = prop(properties, "adminPort", adminPort);
    adminInternalPort = prop(properties, "adminInternalPort", adminInternalPort);
    String start = properties.getProperty("startMode", startMode.name());
    startMode = StartMode.of(prop(properties, "startMode", start));
    String shutdown = properties.getProperty("shutdownMode", shutdownMode.name());
    shutdownMode = StopMode.of(prop(properties, "shutdownMode", shutdown));

    String maxVal = prop(properties, "maxReadyAttempts", null);
    if (maxVal != null) {
      try {
        this.maxReadyAttempts = Integer.parseInt(maxVal);
      } catch (NumberFormatException e) {
        // ignore error
      }
    }
    extraProperties(properties);
    return self();
  }

  /**
   * Override to configure extra properties.
   */
  protected void extraProperties(Properties properties) {
    // nothing by default
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
  @Override
  public SELF containerName(String containerName) {
    this.containerName = containerName;
    return self();
  }

  /**
   * Set the exposed port.
   */
  @Override
  public SELF port(int port) {
    this.port = port;
    return self();
  }

  /**
   * Set the internal (to the container) port.
   */
  @Override
  public SELF internalPort(int internalPort) {
    this.internalPort = internalPort;
    return self();
  }

  /**
   * Set the exposed admin port.
   */
  @Override
  public SELF adminPort(int adminPort) {
    this.adminPort = adminPort;
    return self();
  }

  /**
   * Set the internal admin (to the container) port.
   */
  @Override
  public SELF adminInternalPort(int adminInternalPort) {
    this.adminInternalPort = adminInternalPort;
    return self();
  }

  /**
   * Set the docker image to use.
   */
  @Override
  public SELF image(String image) {
    this.image = image;
    return self();
  }

  /**
   * Set the max attempts to wait for DB ready.
   */
  @Override
  public SELF maxReadyAttempts(int maxReadyAttempts) {
    this.maxReadyAttempts = maxReadyAttempts;
    return self();
  }

  /**
   * Set the docker command to use (defaults to 'docker').
   */
  @Override
  public SELF docker(String docker) {
    this.docker = docker;
    return self();
  }

  /**
   * Return the internal configuration.
   */
  protected InternalConfig internalConfig() {
    return new Inner();
  }

  /**
   * Override to build appropriate jdbc url.
   */
  protected String buildJdbcUrl() {
    throw new IllegalStateException("Not valid for this type");
  }

  protected String buildJdbcAdminUrl() {
    return buildJdbcUrl();
  }

  protected class Inner implements InternalConfig {

    @Override
    public void setDefaultContainerName() {
      if (containerName == null && port != 0) {
        containerName = "ut_" + platform;
      }
    }

    @Override
    public void setContainerId(String containerId) {
      containerName = containerId;
    }

    @Override
    public void setAssignedPort(int assignedPort) {
      port = assignedPort;
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
    public Connection createAdminConnection(String url) throws SQLException {
      throw new IllegalStateException("Not valid for this type");
    }

    @Override
    public int port() {
      return getPort();
    }

    @Override
    public String jdbcUrl() {
      return buildJdbcUrl();
    }

    @Override
    public String jdbcAdminUrl() {
      return buildJdbcAdminUrl();
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
      return "stopping " + platform + " container:" + containerName;
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
    public String getHost() {
      return host;
    }

    @Override
    public int getPort() {
      return port;
    }

    @Override
    public boolean randomPort() {
      return port == 0;
    }

    @Override
    public int getInternalPort() {
      return internalPort;
    }

    @Override
    public int getAdminPort() {
      return adminPort;
    }

    @Override
    public int getAdminInternalPort() {
      return adminInternalPort;
    }

    @Override
    public String getImage() {
      return image;
    }

    @Override
    public StartMode getStartMode() {
      return startMode;
    }

    @Override
    public int getMaxReadyAttempts() {
      return maxReadyAttempts;
    }

    @Override
    public String getDocker() {
      return docker;
    }

    @Override
    public StopMode shutdownMode() {
      return shutdownMode;
    }

    @Override
    public String docker() {
      return docker;
    }

    @Override
    public String image() {
      return image;
    }

  }
}
