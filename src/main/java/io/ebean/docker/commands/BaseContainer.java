package io.ebean.docker.commands;

import io.ebean.docker.commands.process.ProcessHandler;
import io.ebean.docker.container.Container;
import io.ebean.docker.container.ContainerConfig;
import io.ebean.docker.container.StopMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

abstract class BaseContainer implements Container {

  static final Logger log = LoggerFactory.getLogger(Commands.class);

  protected final BaseConfig<?, ?> buildConfig;
  protected InternalConfig config;
  protected final Commands commands;
  protected int waitForConnectivityAttempts = 200;

  BaseContainer(BaseConfig<?, ?> buildConfig) {
    this.buildConfig = buildConfig;
    this.commands = new Commands(buildConfig.docker);
    this.config = buildConfig.internalConfig();
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

    private final StopMode mode;

    Hook(StopMode mode) {
      this.mode = mode;
    }

    @Override
    public void run() {
      if (StopMode.Remove == mode) {
        stopRemove();
      } else {
        stopOnly();
      }
    }
  }

  /**
   * Register a JVM Shutdown hook to stop the container with the given mode.
   */
  public void registerShutdownHook() {
    if (!skipShutdown()) {
      Runtime.getRuntime().addShutdownHook(new Hook(config.shutdownMode()));
    }
  }

  private boolean skipShutdown() {
    return config.checkSkipShutdown() && SkipShutdown.isSkip();
  }

  protected boolean shutdownHook(boolean started) {
    if (StopMode.None != config.shutdownMode()) {
      registerShutdownHook();
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
    log.debug("waitForConnectivity {} max attempts:{} ... ", config.containerName(), waitForConnectivityAttempts);
    for (int i = 0; i < waitForConnectivityAttempts; i++) {
      if (checkConnectivity()) {
        return true;
      }
      try {
        int sleep = (i < 10) ? 10 : (i < 20) ? 20 : 200;
        Thread.sleep(sleep);
        if (i > 200 && i % 100 == 0) {
          log.info("waitForConnectivity {} attempts {} of {} ... ", config.containerName(), i, waitForConnectivityAttempts);
        }
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
    switch (config.getStopMode()) {
      case Remove:
        stopRemove();
        break;
      case Stop:
        stopOnly();
        break;
    }
  }

  /**
   * Stop and remove the container effectively deleting the database.
   */
  public void stopRemove() {
    if (!config.isStopModeNone()) {
      commands.stopRemove(config.containerName());
    }
  }

  /**
   * Stop the container only (no remove).
   */
  @Override
  public void stopOnly() {
    if (!config.isStopModeNone()) {
      commands.stopIfRunning(config.containerName());
    }
  }

  protected ProcessBuilder createProcessBuilder(List<String> args) {
    ProcessBuilder pb = new ProcessBuilder();
    pb.command(args);
    if (log.isDebugEnabled()) {
      log.debug(String.join(" ", args));
    }
    return pb;
  }

  protected List<String> dockerRun() {
    List<String> args = new ArrayList<>();
    args.add(config.docker());
    args.add("run");
    args.add("-d");
    args.add("--name");
    args.add(config.containerName());
    args.add("-p");
    args.add(config.getPort() + ":" + config.getInternalPort());
    return args;
  }

  /**
   * Log that the container is already running.
   */
  void logRunning() {
    log.info("Container {} already running with host:{} port:{}", config.containerName(), config.getHost(), config.getPort());
  }

  /**
   * Log that we are about to run an existing container.
   */
  void logRun() {
    log.info("Run container {} with host:{} port:{}", config.containerName(), config.getHost(), config.getPort());
  }

  /**
   * Log that we are about to start a container.
   */
  void logStart() {
    log.info("Start container {} with host:{} port:{}", config.containerName(), config.getHost(), config.getPort());
  }

  /**
   * Log that the container failed to start.
   */
  void logNotStarted() {
    log.warn("Failed to start container {} with host:{} port:{}", config.containerName(), config.getHost(), config.getPort());
  }

  /**
   * Log that the container has started.
   */
  void logStarted() {
    log.debug("Container {} ready with host:{} port:{}", config.containerName(), config.getHost(), config.getPort());
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

  /**
   * Return http GET content given the url.
   */
  protected String readUrlContent(String url) throws IOException {
    URLConnection yc = new URL(url).openConnection();
    StringBuilder sb = new StringBuilder(300);
    try (BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream(), StandardCharsets.UTF_8))) {
      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        sb.append(inputLine).append("\n");
      }
    }
    return sb.toString();
  }
}
