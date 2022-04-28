package io.ebean.docker.container;

import io.ebean.docker.commands.StartMode;
import io.ebean.docker.commands.StopMode;

import java.util.Properties;

/**
 * Builder for containers.
 */
public interface ContainerBuilder<SELF extends ContainerBuilder<SELF>>  {

  /**
   * Set configuration from properties.
   */
  SELF setProperties(Properties properties);

  /**
   * Set the container name to use.
   */
  SELF setContainerName(String containerName);

  /**
   * Set the exposed port to use.
   */
  SELF setPort(int port);

  /**
   * Set the internal port to map to.
   */
  SELF setInternalPort(int internalPort);

  /**
   * Set the exposed admin port to use.
   */
  SELF setAdminPort(int adminPort);

  /**
   * Set the internal port mapped to the admin port.
   */
  SELF setAdminInternalPort(int adminInternalPort);

  /**
   * Set the docker image to use.
   */
  SELF setImage(String image);

  /**
   * Set the maximum attempts to check ready status.
   */
  SELF setMaxReadyAttempts(int maxReadyAttempts);

  /**
   * Set the docker executable to use. Defaults to docker.
   */
  SELF setDocker(String docker);

  /**
   * Set the start mode.  One of create, dropCreate, or container [only].
   */
  SELF setStartMode(StartMode startMode);

  /**
   * Set the stop mode used when stop() is called.
   */
  SELF setStopMode(StopMode stopMode);

  /**
   * Set the shutdown hook mode to automatically stop/remove the container on JVM shutdown.
   */
  SELF setShutdownMode(StopMode shutdownHookMode);

}
