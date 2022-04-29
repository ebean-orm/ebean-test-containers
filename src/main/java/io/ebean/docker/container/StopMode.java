package io.ebean.docker.container;

public enum StopMode {
  Stop, None, Remove;

  public static StopMode of(String mode) {
    if ("remove".equalsIgnoreCase(mode)) {
      return Remove;
    }
    if ("none".equalsIgnoreCase(mode)) {
      return None;
    }
    return Stop;
  }
}
