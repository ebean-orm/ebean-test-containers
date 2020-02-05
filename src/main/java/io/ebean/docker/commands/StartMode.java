package io.ebean.docker.commands;

public enum StartMode {
  Create,
  DropCreate,
  Container;

  public static StartMode of(String val) {
    if ("dropCreate".equalsIgnoreCase(val)) {
      return DropCreate;
    }
    if ("container".equalsIgnoreCase(val)) {
      return Container;
    }
    // default start mode
    return Create;
  }

}
