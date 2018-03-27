package io.ebean.docker.container;

/**
 * Commands for starting and stopping a DB container.
 */
public interface Container {

  /**
   * Returns the container configuration.
   */
  ContainerConfig config();

  /**
   * Start the container.
   */
  boolean start();

  /**
   * Stop the container using stopMode which defaults to stop and remove.
   */
  void stop();

  /**
   * Stop the container only (no remove).
   */
  void stopOnly();

}
