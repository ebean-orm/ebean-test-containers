package io.ebean.test.containers;

/**
 * Commands for starting and stopping a DB container.
 */
public interface Container<C extends Container<C>> {

  /**
   * Returns the container configuration.
   */
  ContainerConfig config();

  /**
   * Start and return the container.
   * <p>
   * Throws an IllegalStateException if the container can not be started.
   */
  C start();

  /**
   * Start the container or throw a IllegalStateException.
   */
  void startOrThrow();

  /**
   * Start the container returning true if successful.
   */
  boolean startMaybe();

  /**
   * Stop the container.
   */
  void stop();

  /**
   * Stop and remove the container .
   */
  void stopRemove();

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
