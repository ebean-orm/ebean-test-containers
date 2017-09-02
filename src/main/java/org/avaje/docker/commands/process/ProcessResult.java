package org.avaje.docker.commands.process;

import org.avaje.docker.commands.CommandException;

import java.util.List;

public class ProcessResult {

  final int result;
  final List<String> stdOutLines;
  final List<String> stdErrLines;

  public ProcessResult(int result, List<String> stdOutLines, List<String> stdErrLines) {
    this.result = result;
    this.stdOutLines = stdOutLines;
    this.stdErrLines = stdErrLines;
  }

  public int getResult() {
    return result;
  }

  public List<String> getStdOutLines() {
    return stdOutLines;
  }

  public List<String> getStdErrLines() {
    return stdErrLines;
  }

  public String stdOut() {
    return lines(stdOutLines);
  }

  public String stdErr() {
    return lines(stdErrLines);
  }

  /**
   * Return debug output.
   */
  public String debug() {
    return "exit:" + result + "\n out:" + stdOut() + "\n err:" + stdErr();
  }

  private String lines(List<String> lines) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < lines.size(); i++) {
      if (i > 0) {
        sb.append("\n");
      }
      sb.append(lines.get(i));
    }
    return sb.toString();
  }

  public boolean success() {
    return result == 0;
  }

  public void reportFail(String message) {
    if (!success()) {
      System.err.println("DEBUG:"+debug());
      throw new CommandException(message, this);
    }
  }
}
