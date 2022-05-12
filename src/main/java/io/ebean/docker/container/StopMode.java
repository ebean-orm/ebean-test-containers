package io.ebean.docker.container;

public enum StopMode {

  /**
   * Do nothing.
   */
  None,

  /**
   * Stop the container.
   */
  Stop,

  /**
   * Stop and remove the container.
   */
  Remove,

  /**
   * Shutdown and remove the container by default unless there is a ~/.ebean/ignore-docker-shutdown marker file.
   */
  Auto;

  public static StopMode of(String mode) {
    if ("remove".equalsIgnoreCase(mode)) {
      return Remove;
    }
    if ("none".equalsIgnoreCase(mode)) {
      return None;
    }
    if ("auto".equalsIgnoreCase(mode)) {
      return Auto;
    }
    return Stop;
  }
}
