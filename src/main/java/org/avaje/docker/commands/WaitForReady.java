package org.avaje.docker.commands;

public interface WaitForReady {

  /**
   * Wait for a ready state on a container.
   */
  boolean waitForReady();

}
