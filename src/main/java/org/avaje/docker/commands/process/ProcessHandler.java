package org.avaje.docker.commands.process;

import org.avaje.docker.commands.CommandException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
      new LineRead(this::readStdOut, process.getInputStream()).read();
      new LineRead(this::readStdErr, process.getErrorStream()).read();

      int result = process.waitFor();
      ProcessResult pr = new ProcessResult(result, stdOutLines, stdErrLines);
      if (!pr.success() && log.isTraceEnabled()) {
        log.trace(pr.debug());
      }
      return pr;

    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void readStdErr(String line) {
    processLine(line, stdErrLines);
  }

  private void readStdOut(String line) {
    processLine(line, stdOutLines);
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

  /**
   * Read the content into lines. This replaces normal readLine() which can hang on sql server docker logs.
   */
  static class LineRead {

    private Consumer<String> consumer;

    private BufferedReader in;

    private StringBuilder lineBuffer = new StringBuilder(400);

    LineRead(Consumer<String> consumer, InputStream input) {
      this.consumer = consumer;
      this.in = new BufferedReader(new InputStreamReader(input));
    }

    private void processBuffer(char[] buffer, int len) {

      for (int i = 0; i < len; i++) {
        if (buffer[i] == '\n' || buffer[i] == '\r') {
          String line = lineBuffer.toString();
          if (line.length() > 0) {
            consumer.accept(line);
            lineBuffer = new StringBuilder(400);
          }
        } else {
          lineBuffer.append(buffer[i]);
        }
      }
    }

    private void read() {

      try {
        char[] buffer = new char[1024];
        for (; ; ) {
          int len = in.read(buffer);
          if (len < 0) {
            break;
          }
          processBuffer(buffer, len);
        }
      } catch (IOException e) {
        log.error("Error reading input", e);

      } finally {
        try {
          in.close();
        } catch (IOException e) {
          log.error("Error reading input", e);
        }
      }
    }
  }
}
