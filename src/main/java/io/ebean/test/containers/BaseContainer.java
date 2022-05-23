package io.ebean.test.containers;

import io.ebean.test.containers.process.ProcessHandler;
import io.ebean.test.containers.process.ProcessResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.System.Logger.Level;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

abstract class BaseContainer implements Container {

  static final System.Logger log = Commands.log;

  protected final BaseConfig<?, ?> buildConfig;
  protected InternalConfig config;
  protected final Commands commands;
  protected int waitForConnectivityAttempts = 200;
  protected StopMode shutdownMode;
  protected boolean usingContainerId;
  protected boolean usingRandomPort;
  protected boolean removeOnExit;

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
    setDefaultContainerName();
    return shutdownHook(logStarted(startWithConnectivity()));
  }

  @Override
  public int port() {
    return config.getPort();
  }

  /**
   * Set a default container name if not using random port.
   */
  protected void setDefaultContainerName() {
    config.setDefaultContainerName();
    shutdownMode = determineShutdownMode();
  }

  private StopMode determineShutdownMode() {
    StopMode mode = config.shutdownMode();
    if (mode == StopMode.Auto) {
      if (config.randomPort()) {
        return StopMode.Stop;
      } else if (SkipShutdown.isSkip()) {
        return StopMode.None;
      } else {
        return StopMode.Remove;
      }
    }
    return mode;
  }

  @Override
  public boolean isRunning() {
    return commands.isRunning(config.containerName());
  }

  private static final AtomicInteger hookCounter = new AtomicInteger();

  private class Hook extends Thread {

    private final StopMode mode;

    Hook(StopMode mode) {
      super("shutdown" + hookCounter.getAndIncrement());
      this.mode = mode;
    }

    @Override
    public void run() {
      if (StopMode.Remove == mode) {
        log.log(Level.INFO, "Stop remove container {0}", config.containerName());
        stopRemove();
      } else {
        log.log(Level.INFO, "Stop container {0}", config.containerName());
        stopIfRunning();
      }
    }
  }

  /**
   * Register a JVM Shutdown hook to stop the container with the given mode.
   */
  public void registerShutdownHook() {
    if (shutdownMode == StopMode.Stop || shutdownMode == StopMode.Remove) {
      Runtime.getRuntime().addShutdownHook(new Hook(shutdownMode));
    }
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
      log.log(Level.WARNING, "Container {0} failed to start - waiting for connectivity", config.containerName());
      return false;
    }
    return true;
  }

  /**
   * Start the container checking if it is already running.
   * Return true if the container is already running.
   */
  boolean startIfNeeded() {
    boolean hasContainerName = hasContainerName();
    if (hasContainerName && commands.isRunning(config.containerName())) {
      checkPort(true);
      logRunning();
      return true;
    }
    if (hasContainerName && commands.isRegistered(config.containerName())) {
      checkPort(false);
      startContainer();
      logStart();
    } else {
      runContainer();
      logRun();
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
    ProcessResult result = ProcessHandler.process(runProcess());
    if (log.isLoggable(Level.DEBUG)) {
      log.log(Level.DEBUG, "run output {0}", result.getOutLines());
    }
    if (!hasContainerName()) {
      usingContainerId = true;
      parseContainerId(result.getOutLines());
    }
    if (usingRandomPort) {
      obtainPort();
    }
  }

  private void parseContainerId(List<String> outLines) {
    if (outLines == null || outLines.isEmpty()) {
      throw new IllegalStateException("Expected docker run output to contain containerId but got [" + outLines + "]");
    }
    config.setContainerId(outLines.get(outLines.size() - 1).trim());
  }

  private void obtainPort() {
    int assignedPort = commands.port(config.containerName());
    if (assignedPort == 0) {
      throw new IllegalStateException("Unable to determine assigned port for containerId [" + config.containerName() + "]");
    }
    log.log(Level.DEBUG, "Container {0} using port {1,number,#}", config.containerName(), assignedPort);
    config.setAssignedPort(assignedPort);
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
    log.log(Level.DEBUG, "waitForConnectivity {0} max attempts:{1} ... ", config.containerName(), waitForConnectivityAttempts);
    for (int i = 0; i < waitForConnectivityAttempts; i++) {
      if (checkConnectivity()) {
        return true;
      }
      try {
        int sleep = (i < 10) ? 10 : (i < 20) ? 20 : 200;
        Thread.sleep(sleep);
        if (i > 200 && i % 100 == 0) {
          log.log(Level.INFO, "waitForConnectivity {0} attempts {1} of {2} ... ", config.containerName(), i, waitForConnectivityAttempts);
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
    stopIfRunning();
  }

  void stopIfRunning() {
    commands.stopIfRunning(config.containerName(), usingContainerId);
  }

  /**
   * Stop and remove the container effectively deleting the container.
   */
  @Override
  public void stopRemove() {
    stopIfRunning();
    if (!removeOnExit) {
      commands.removeIfRegistered(config.containerName(), usingContainerId);
    }
  }

  protected ProcessBuilder createProcessBuilder(List<String> args) {
    ProcessBuilder pb = new ProcessBuilder();
    pb.command(args);
    if (log.isLoggable(Level.DEBUG)) {
      log.log(Level.DEBUG, String.join(" ", args));
    }
    return pb;
  }

  protected List<String> dockerRun() {
    usingRandomPort = config.randomPort();
    List<String> args = new ArrayList<>();
    args.add(config.docker());
    args.add("run");
    args.add("-d");
    if (hasContainerName()) {
      args.add("--name");
      args.add(config.containerName());
    } else if (usingRandomPort) {
      removeOnExit = true;
      args.add("--rm");
    }
    if (usingRandomPort) {
      args.add("-p");
      args.add(String.valueOf(config.getInternalPort()));
    } else {
      args.add("-p");
      args.add(config.getPort() + ":" + config.getInternalPort());
    }
    return args;
  }

  boolean notEmpty(String value) {
    return value != null && !value.isEmpty();
  }

  boolean hasContainerName() {
    return notEmpty(config.containerName());
  }

  /**
   * Log that the container is already running.
   */
  void logRunning() {
    log.log(Level.INFO, "Container {0} already running with host:{1} port:{2,number,#}", config.containerName(), config.getHost(), config.getPort());
  }

  /**
   * Log that we are about to run an existing container.
   */
  void logRun() {
    log.log(Level.INFO, "Run container {0} with host:{1} port:{2,number,#} shutdownMode:{3}", logContainerName(), config.getHost(), config.getPort(), logContainerShutdown());
  }

  String logContainerShutdown() {
    return shutdownMode + (usingContainerId ? " id:" + config.containerName() : "");
  }

  String logContainerName() {
    return usingContainerId ? config.image() : config.containerName();
  }

  /**
   * Log that we are about to start a container.
   */
  void logStart() {
    log.log(Level.INFO, "Start container {0} with host:{1} port:{2,number,#}", config.containerName(), config.getHost(), config.getPort());
  }

  /**
   * Log that the container failed to start.
   */
  void logNotStarted() {
    log.log(Level.WARNING, "Failed to start container {0} with host:{1} port:{2,number,#}", config.containerName(), config.getHost(), config.getPort());
  }

  /**
   * Log that the container has started.
   */
  void logStarted() {
    log.log(Level.DEBUG, "Container {0} ready with host:{1} port:{2,number,#}", config.containerName(), config.getHost(), config.getPort());
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
