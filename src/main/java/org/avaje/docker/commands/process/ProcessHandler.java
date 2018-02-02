package org.avaje.docker.commands.process;

import org.avaje.docker.commands.CommandException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Handle the external process response (exit code, std out, std err).
 */
public class ProcessHandler {

  private static final Logger log = LoggerFactory.getLogger(ProcessHandler.class);

  private final ProcessBuilder builder;

  private Process process;

  private List<String> stdOutLines = new ArrayList<>();
  private List<String> stdErrLines = new ArrayList<>();

  private String match;
  private String clearMatch;


  private ProcessHandler(ProcessBuilder builder, String match, String clearMatch) {
    this.builder = builder;
    this.match = match;
    this.clearMatch = clearMatch;
  }

  private void start() throws IOException {
    process = builder.start();
  }

  public static ProcessResult matchCommand(String match, String clearMatch, String... command) {
    return process(new ProcessBuilder(command), match, clearMatch);
  }

  /**
   * Process a basic command.
   */
  public static ProcessResult command(String... command) {
    return process(new ProcessBuilder(command));
  }

  public static ProcessResult process(ProcessBuilder pb) {
    return process(pb, null, null);
  }

  /**
   * Process a command.
   */
  private static ProcessResult process(ProcessBuilder pb, String match, String clearMatch) {
    try {
      ProcessHandler handler = new ProcessHandler(pb, match, clearMatch);
      handler.start();
      ProcessResult result = handler.read();
      if (!result.success()) {
        throw new CommandException("command failed: " + result.getStdErrLines(), result);
      }
      return result;

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private ProcessResult read() {

    try {
      BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
      BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

      String s;
      while ((s = stdError.readLine()) != null) {
        processLine(s, stdErrLines);
      }
      while ((s = stdInput.readLine()) != null) {
        processLine(s, stdOutLines);
      }

      int result = process.waitFor();
      ProcessResult pr = new ProcessResult(result, stdOutLines, stdErrLines);
      if (!pr.success() && log.isTraceEnabled()) {
        log.trace(pr.debug());
      }
      return pr;

    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void processLine(String s, List<String> lines) {
    if (clearMatch != null && s.contains(clearMatch)) {
      stdOutLines.clear();
      stdErrLines.clear();
    } else {
      if (match != null) {
        if (s.contains(match)) {
          stdOutLines.add(s);
        }
      } else {
        lines.add(s);
      }
    }
  }

}
