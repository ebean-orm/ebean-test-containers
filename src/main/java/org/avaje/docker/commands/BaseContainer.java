package org.avaje.docker.commands;

import org.avaje.docker.commands.process.ProcessHandler;
import org.avaje.docker.container.Container;
import org.avaje.docker.container.ContainerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

abstract class BaseContainer implements Container {

  static final Logger log = LoggerFactory.getLogger(Commands.class);

  protected final BaseConfig config;

  protected final Commands commands;

  BaseContainer(BaseConfig config) {
    this.config = config;
    this.commands = new Commands(config.docker);
  }

  /**
   * Return the ProcessBuilder used to execute the container run command.
   */
  protected abstract ProcessBuilder runProcess();

  @Override
  public ContainerConfig config() {
    return config;
  }

  @Override
  public boolean start() {
    return startWithConnectivity();
  }

  protected boolean startWithConnectivity() {
    startIfNeeded();
    if (!waitForConnectivity()) {
      log.warn("Container {} failed to start - waiting for connectivity", config.containerName());
      return false;
    }
    logStarted();
    return true;
  }

  /**
   * Start the container checking if it is already running.
   */
  void startIfNeeded() {

    if (!commands.isRunning(config.containerName())) {
      if (commands.isRegistered(config.containerName())) {
        commands.start(config.containerName());

      } else {
        log.debug("run {} container {}", config.platform(), config.containerName());
        runContainer();
      }
    }
  }

  void runContainer() {
    ProcessHandler.process(runProcess());
  }

  /**
   * Return true if the docker container logs contain the match text.
   */
  boolean logsContain(String match) {
    return commands.logsContain(config.containerName(), match);
  }

  abstract boolean checkConnectivity();

  /**
   * Return true when we can make IP connections to the database (JDBC).
   */
  boolean waitForConnectivity() {
    for (int i = 0; i < 120; i++) {
      if (checkConnectivity()) {
        return true;
      }
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return false;
  }

  /**
   * Stop using the configured stopMode of 'stop' or 'remove'.
   * <p>
   * Remove additionally removes the container (expected use in build agents).
   */
  @Override
  public void stop() {
    String mode = config.getStopMode().toLowerCase().trim();
    switch (mode) {
      case "stop":
        stopOnly();
        break;
      case "remove":
        stopRemove();
        break;
      default:
        stopOnly();
    }
  }

  /**
   * Stop and remove the container effectively deleting the database.
   */
  public void stopRemove() {
    commands.stopRemove(config.containerName());
  }

  /**
   * Stop the container only (no remove).
   */
  @Override
  public void stopOnly() {
    commands.stopIfRunning(config.containerName());
  }

  protected ProcessBuilder createProcessBuilder(List<String> args) {
    ProcessBuilder pb = new ProcessBuilder();
    pb.command(args);
    if (log.isDebugEnabled()) {
      log.debug(String.join(" ", args));
    }
    return pb;
  }

  /**
   * Log a message for container starting or not.
   */
  boolean logStart(boolean started) {
    if (started) {
      logStarted();
    } else {
      logNotStarted();
    }
    return started;
  }

  void logStarted() {
    log.info("Started container {} with port:{}", config.containerName(), config.getPort());
  }

  void logNotStarted() {
    log.warn("Failed to start container {} with port {}", config.containerName(), config.getPort());
  }
}
