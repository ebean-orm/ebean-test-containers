package io.ebean.docker.commands;

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
