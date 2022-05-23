package io.ebean.test.containers;

import io.ebean.test.containers.process.ProcessHandler;
import io.ebean.test.containers.process.ProcessResult;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Common Docker container commands.
 */
public class Commands {

  static final System.Logger log = System.getLogger("io.ebean.test.containers");

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
  public void stopIfRunning(String containerName, boolean skipRunningCheck) {
    if (skipRunningCheck || isRunning(containerName)) {
      stop(containerName);
    }
  }

  /**
   * Stop and remove the container.
   */
  public void removeIfRegistered(String containerName, boolean skipRunningCheck) {
    if (skipRunningCheck || isRegistered(containerName)) {
      remove(containerName);
    }
  }

  /**
   * Remove the container.
   */
  public void remove(String containerName) {
    log.log(Level.DEBUG, "remove {0}", containerName);
    ProcessHandler.command(docker, "rm", containerName);
  }

  /**
   * Start the container.
   */
  public void start(String containerName) {
    log.log(Level.DEBUG, "start {0}", containerName);
    ProcessHandler.command(docker, "start", containerName);
  }

  /**
   * Stop the container.
   */
  public void stop(String containerName) {
    log.log(Level.DEBUG, "stop {0}", containerName);
    try {
      ProcessHandler.command(docker, "stop", containerName);
    } catch (CommandException e) {
      if (e.getMessage().contains("No such container")) {
        log.log(Level.TRACE,"container not running {0}", containerName);
      } else {
        log.log(Level.INFO, "Error stopping container - " + e.getMessage());
      }
    }
  }

  public void removeContainers(String... containerNames) {
    log.log(Level.DEBUG, "remove {0}", Arrays.toString(containerNames));
    try {
      dockerCmd("rm", containerNames);
    } catch (CommandException e) {
      log.log(Level.DEBUG, "removing containers that don't exist " + e.getMessage());
    }
  }

  public void stopContainers(String... containerNames) {
    log.log(Level.DEBUG, "stop {0}", Arrays.toString(containerNames));
    try {
      dockerCmd("stop", containerNames);
    } catch (CommandException e) {
      log.log(Level.DEBUG, "stopping containers that don't exist " + e.getMessage());
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
   * Return the first mapped port for the given container.
   */
  public int port(String containerName) {
    List<String> lines = ports(containerName);
    for (String line : lines) {
      int port = parsePort(line);
      if (port > 0) {
        return port;
      }
    }
    return 0;
  }

  static int parsePort(String line) {
    int pos0 = line.indexOf("->");
    if (pos0 > -1) {
      String substring = line.substring(pos0 + 1);
      int lastColon = substring.lastIndexOf(':');
      if (lastColon > -1) {
        return Integer.parseInt(substring.substring(lastColon + 1));
      }
    }
    return 0;
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
   * Return the ports output as lines.
   */
  public List<String> ports(String containerName) {
    ProcessResult result = ProcessHandler.command(docker, "port", containerName);
    return result.getOutLines();
  }

  /**
   * Check if the port matches the existing port bindings and if not return the existing port bindings.
   */
  public String registeredPortMatch(String containerName, int matchPort) {
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
