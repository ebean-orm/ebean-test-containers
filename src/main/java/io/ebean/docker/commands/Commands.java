package io.ebean.docker.commands;

import io.ebean.docker.commands.process.ProcessHandler;
import io.ebean.docker.commands.process.ProcessResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Common Docker container commands.
 */
public class Commands {

  private static final Logger log = LoggerFactory.getLogger(Commands.class);

  private final String docker;

  /**
   * Create with 'docker' as the command.
   */
  public Commands() {
    this("docker");
  }

  /**
   * Construct with explicit docker command.
   */
  public Commands(String docker) {
    this.docker = docker;
  }

  /**
   * Stop the container checking to see if it is running first.
   */
  public void stopIfRunning(String containerName) {

    log.debug("stopIfRunning {}", containerName);
    if (isRunning(containerName)) {
      stop(containerName);
    }
  }

  /**
   * Stop and remove the container.
   */
  public void stopRemove(String containerName) {

    log.debug("stopRemove {}", containerName);
    if (isRunning(containerName)) {
      stop(containerName);
    }

    if (isRegistered(containerName)) {
      remove(containerName);
    }
  }

  /**
   * Remove the container.
   */
  public void remove(String containerName) {
    log.debug("remove {}", containerName);
    ProcessHandler.command(docker, "rm", containerName);
  }

  /**
   * Start the container.
   */
  public void start(String containerName) {
    log.debug("start {}", containerName);
    ProcessHandler.command(docker, "start", containerName);
  }

  /**
   * Stop the container.
   */
  public void stop(String containerName) {
    log.debug("stop {}", containerName);
    ProcessHandler.command(docker, "stop", containerName);
  }

  public void removeContainers(String... containerNames) {
    log.debug("remove {}", Arrays.toString(containerNames));
    try {
      dockerCmd("rm", containerNames);
    } catch (CommandException e) {
      log.debug("removing containers that don't exist " + e.getMessage());
    }
  }

  public void stopContainers(String... containerNames) {
    log.debug("stop {}", Arrays.toString(containerNames));
    try {
      dockerCmd("stop", containerNames);
    } catch (CommandException e) {
      log.debug("stopping containers that don't exist " + e.getMessage());
    }
  }

  private void dockerCmd(String cmd, String[] containerNames) {
    final List<String> cmds = new ArrayList<>();
    cmds.add(docker);
    cmds.add(cmd);
    for (String containerName : containerNames) {
      cmds.add(containerName);
    }
    ProcessHandler.command(cmds);
  }

  /**
   * Return true if the container is running.
   */
  public boolean isRunning(String containerName) {
    return running().contains(containerName);
  }

  /**
   * Return true if the container is registered (exists and maybe running or not).
   */
  public boolean isRegistered(String containerName) {
    return registered().contains(containerName);
  }

  /**
   * Return true if the logs of the container contain the match text.
   */
  public boolean logsContain(String containerName, String match) {
    return logsContain(containerName, match, null);
  }

  public boolean logsContain(String containerName, String match, String clearMatch) {
    List<String> matchLines = logsWithMatch(containerName, match, clearMatch);
    return !matchLines.isEmpty();
  }

  /**
   * Return true if the logs of the container contain the match text.
   */
  public List<String> logsWithMatch(String containerName, String match, String clearMatch) {
    ProcessResult result = ProcessHandler.matchCommand(match, clearMatch, docker, "logs", containerName);
    return result.getOutLines();
  }

  /**
   * Return true if the logs of the container contain the match text.
   */
  public List<String> logs(String containerName) {
    ProcessResult result = ProcessHandler.command(docker, "logs", containerName);
    return result.getOutLines();
  }

  /**
   * Return the list of containers currently running.
   */
  private List<String> running() {
    ProcessResult result = ProcessHandler.command(docker, "ps", "--format", "{{.Names}}");
    return result.getOutLines();
  }

  /**
   * Return the list of containers which maybe running or not.
   */
  private List<String> registered() {
    ProcessResult result = ProcessHandler.command(docker, "ps", "-a", "--format", "{{.Names}}");
    return result.getOutLines();
  }

  /**
   * Check if the port matches the existing port bindings and if not return the existing port bindings.
   */
  public String registeredPortMatch(String containerName, String matchPort) {
    ProcessResult result = ProcessHandler.command(docker, "container", "inspect", containerName, "--format={{.HostConfig.PortBindings}}");
    List<String> outLines = result.getOutLines();
    for (String outLine : outLines) {
      if (outLine.startsWith("map")) {
        if (outLine.contains("{ " + matchPort + "}")) {
          // port matching all good
          return null;
        } else {
          // mismatch - return all the PortBindings to include in exception message
          return outLine;
        }
      }
    }
    // container doesn't exist
    return null;
  }

  /**
   * Return true if the container logs contains the logMessage.
   * <p>
   * Usually used to find a log entry to indicate the container is ready for use.
   * </p>
   *
   * @param containerName The container logs to search
   * @param logMessage    The logMessage we are looking for (to usually indicate the container is ready).
   * @param tail          The number of logs to tail (such that we get recent and minimal logs)
   * @return True if the logs searched contain the logMessage
   */
  public boolean logsContain(String containerName, String logMessage, int tail) {

    List<String> lines = logs(containerName, tail);
    for (String line : lines) {
      if (line.contains(logMessage)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return the logs for the container with a tail.
   * <p>
   * If tail = 0 then all logs are returned (but we should be careful using that).
   * </p>
   */
  public List<String> logs(String containerName, int tail) {

    ProcessResult result;
    if (tail > 0) {
      result = ProcessHandler.command(docker, "logs", "--tail", Integer.toString(tail), containerName);
    } else {
      result = ProcessHandler.command(docker, "logs", containerName);
    }
    return result.getOutLines();
  }

}
