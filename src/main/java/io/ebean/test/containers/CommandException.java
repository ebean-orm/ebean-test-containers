package io.ebean.test.containers;

import io.ebean.test.containers.process.ProcessResult;

public class CommandException extends RuntimeException {

  private final ProcessResult result;

  public CommandException(String message, ProcessResult result) {
    super(message);
    this.result = result;
  }

  public ProcessResult getResult() {
    return result;
  }

  @Override
  public String toString() {
    return result.toString();
  }
}
