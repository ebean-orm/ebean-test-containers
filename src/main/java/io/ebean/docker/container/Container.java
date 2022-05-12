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

  /**
   * Return true if the container is running.
   */
  boolean isRunning();

  /**
   * Return the port this container is using.
   * <p>
   * This is typically useful if the container was started with a random port
   * and, we need to know what that port was.
   */
  int port();
}
