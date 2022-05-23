package io.ebean.docker.commands.process;

import io.ebean.docker.commands.CommandException;
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

  /**
   * Both stdErr and stdOut merged.
   */
  private List<String> out = new ArrayList<>();

  private String match;
  private String clearMatch;


  private ProcessHandler(ProcessBuilder builder, String match, String clearMatch) {
    this.builder = builder;
    this.match = match;
    this.clearMatch = clearMatch;
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

  public static ProcessResult command(List<String> commands) {
    return process(new ProcessBuilder(commands));
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
        throw new CommandException("command failed: " + result.getOutLines(), result);
      }
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void start() throws IOException {
    // merge input and error streams
    builder.redirectErrorStream(true);
    process = builder.start();
  }

  private ProcessResult read() {
    try {
      BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String s;
      while ((s = stdInput.readLine()) != null) {
        processLine(s, out);
      }

      int result = process.waitFor();
      ProcessResult pr = new ProcessResult(result, out);
      if (!pr.success() && log.isTraceEnabled()) {
        log.trace(pr.debug());
      }
      return pr;

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void processLine(String lineContent, List<String> lines) {
    if (clearMatch != null && lineContent.contains(clearMatch)) {
      out.clear();
    } else {
      if (match != null) {
        if (lineContent.contains(match)) {
          out.add(lineContent);
        }
      } else {
        lines.add(lineContent);
      }
    }
  }

}
