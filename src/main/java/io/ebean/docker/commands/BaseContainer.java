package io.ebean.docker.commands;

import io.ebean.docker.commands.process.ProcessHandler;
import io.ebean.docker.container.Container;
import io.ebean.docker.container.ContainerConfig;
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
    return shutdownHook(logStarted(startWithConnectivity()));
  }

  public boolean isRunning() {
    return commands.isRunning(config.containerName());
  }

  private class Hook extends Thread {

    private final String mode;

    Hook(String mode) {
      this.mode = mode;
    }

    @Override
    public void run() {
      if ("remove".equalsIgnoreCase(mode)) {
        stopRemove();
      } else {
        stopOnly();
      }
    }
  }

  /**
   * Register a JVM Shutdown hook to stop the container with the given mode.
   *
   * @param mode If "remove" then stop and remove the container and otherwise just stop the container.
   */
  public void registerShutdownHook(String mode) {
    Runtime.getRuntime().addShutdownHook(new Hook(mode));
  }

  protected boolean shutdownHook(boolean started) {

    String mode = config.shutdownMode();
    if (mode != null && !mode.equalsIgnoreCase("none")) {
      registerShutdownHook(mode);
    }
    return started;
  }

  protected boolean startWithConnectivity() {
    startIfNeeded();
    if (!waitForConnectivity()) {
      log.warn("Container {} failed to start - waiting for connectivity", config.containerName());
      return false;
    }
    return true;
  }

  /**
   * Start the container checking if it is already running.
   * Return true if the container is already running.
   */
  boolean startIfNeeded() {
    if (commands.isRunning(config.containerName())) {
      checkPort(true);
      logRunning();
      return true;
    }

    if (commands.isRegistered(config.containerName())) {
      checkPort(false);
      logStart();
      startContainer();

    } else {
      logRun();
      runContainer();
    }
    return false;
  }

  void startContainer() {
    commands.start(config.containerName());
  }

  private void checkPort(boolean isRunning) {
    String portBindings = commands.registeredPortMatch(config.containerName(), config.getPort());
    if (portBindings != null) {
      String msg = "The existing port bindings [" + portBindings + "] for this docker container [" + config.containerName()
        + "] don't match the configured port [" + config.getPort()
        + "] so it seems the port has changed? Maybe look to remove the container first if you want to use the new port via:";
      if (isRunning) {
        msg += "    docker stop " + config.containerName();
      }
      msg += "    docker rm " + config.containerName();
      throw new IllegalStateException(msg);
    }
  }

  void runContainer() {
    ProcessHandler.process(runProcess());
  }

  /**
   * Return true if the docker container logs contain the match text.
   */
  boolean logsContain(String match, String clearMatch) {
    return logsContain(config.containerName(), match, clearMatch);
  }

  boolean logsContain(String containerName, String match, String clearMatch) {
    return commands.logsContain(containerName, match, clearMatch);
  }

  /**
   * Return all the logs from the container (can be big, be careful).
   */
  List<String> logs() {
    return commands.logs(config.containerName());
  }

  abstract boolean checkConnectivity();

  /**
   * Return true when we can make IP connections to the database (JDBC).
   */
  boolean waitForConnectivity() {
    log.debug("waitForConnectivity {} ... ", config.containerName());
    for (int i = 0; i < 200; i++) {
      if (checkConnectivity()) {
        return true;
      }
      try {
        int sleep = (i < 10) ? 10 : (i < 20) ? 20 : 200;
        Thread.sleep(sleep);
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
   * Log that the container is already running.
   */
  void logRunning() {
    log.info("Container {} already running with port:{}", config.containerName(), config.getPort());
  }

  /**
   * Log that we are about to run an existing container.
   */
  void logRun() {
    log.info("Run container {} with port:{}", config.containerName(), config.getPort());
  }

  /**
   * Log that we are about to start a container.
   */
  void logStart() {
    log.info("Start container {} with port:{}", config.containerName(), config.getPort());
  }

  /**
   * Log that the container failed to start.
   */
  void logNotStarted() {
    log.warn("Failed to start container {} with port {}", config.containerName(), config.getPort());
  }

  /**
   * Log that the container has started.
   */
  void logStarted() {
    log.debug("Container {} ready with port {}", config.containerName(), config.getPort());
  }

  /**
   * Log a message after the container has started or not.
   */
  boolean logStarted(boolean started) {
    if (!started) {
      logNotStarted();
    } else {
      logStarted();
    }
    return started;
  }
}
