package org.avaje.docker.commands.process;

import java.util.List;

/**
 * The result of an external process call.
 */
public class ProcessResult {

  private final int result;

  private final List<String> out;

  /**
   * Create with the result exit code and std out and err content.
   */
  public ProcessResult(int result, List<String> out) {
    this.result = result;
    this.out = out;
  }

  /**
   * Return true if exit result was 0.
   */
  public boolean success() {
    return result == 0;
  }

  /**
   * Return the STD OUT lines.
   */
  public List<String> getOutLines() {
    return out;
  }

  /**
   * Return all the stdOut and stdErr content (merged).
   */
  private String out() {
    return lines(out);
  }

  /**
   * Return debug output.
   */
  public String debug() {
    return "exit:" + result + "\n out:" + out(); // + "\n err:" + stdErr();
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

}
