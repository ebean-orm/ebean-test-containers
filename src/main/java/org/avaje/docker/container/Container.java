package org.avaje.docker.container;

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
   * Stop the container.
   */
  void stop();

}
