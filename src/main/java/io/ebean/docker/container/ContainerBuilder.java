package io.ebean.docker.container;

import java.util.Properties;

/**
 * Builder for containers.
 */
public interface ContainerBuilder<C,SELF extends ContainerBuilder<C,SELF>>  {

  /**
   * Build the container.
   */
  C build();

  /**
   * Set configuration from properties.
   */
  SELF properties(Properties properties);

  /**
   * Set the container name to use.
   */
  SELF containerName(String containerName);

  /**
   * Set the exposed port to use.
   */
  SELF port(int port);

  /**
   * Set the internal port to map to.
   */
  SELF internalPort(int internalPort);

  /**
   * Set the exposed admin port to use.
   */
  SELF adminPort(int adminPort);

  /**
   * Set the internal port mapped to the admin port.
   */
  SELF adminInternalPort(int adminInternalPort);

  /**
   * Set the docker image to use.
   */
  SELF image(String image);

  /**
   * Set the maximum attempts to check ready status.
   */
  SELF maxReadyAttempts(int maxReadyAttempts);

  /**
   * Set the docker executable to use. Defaults to docker.
   */
  SELF docker(String docker);

  /**
   * Set the start mode.  One of create, dropCreate, or container [only].
   */
  SELF startMode(StartMode startMode);

  /**
   * Set the shutdown hook mode to automatically stop/remove the container on JVM shutdown.
   */
  SELF shutdownMode(StopMode shutdownHookMode);

}
