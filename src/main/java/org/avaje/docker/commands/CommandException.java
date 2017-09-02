package org.avaje.docker.commands;

import org.avaje.docker.commands.process.ProcessResult;

public class CommandException extends RuntimeException {

  private ProcessResult result;

  public CommandException(String message, ProcessResult result) {
    super(message);
    this.result = result;
  }

  public ProcessResult getResult() {
    return result;
  }
}
