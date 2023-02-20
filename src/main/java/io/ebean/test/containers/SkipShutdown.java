package io.ebean.test.containers;

import java.io.File;

/**
 * Detect if we should skip container stop/stopRemove for local development.
 * <p>
 * Developers put a marker file <code>~/.ebean/ignore-docker-shutdown</code> which is
 * detected and keeps the docker container running for the next test run.
 */
public class SkipShutdown {

  /**
   * Return true to skip docker container stop for local development.
   * <p>
   */
  public static boolean isSkip() {
    return new SkipShutdown().ignoreDockerShutdown();
  }

  /**
   * For local development we most frequently want to ignore docker shutdown.
   * <p>
   * So we just want the shutdown mode to be used on the CI server.
   */
  boolean ignoreDockerShutdown() {
    return ignoreDockerShutdown(ignoreMarkerFile());
  }

  static String ignoreMarkerFile() {
    return System.getProperty("ebean.test.localDevelopment", "~/.ebean/ignore-docker-shutdown");
  }

  boolean ignoreDockerShutdown(String localDev) {
    if (localDev.startsWith("~/")) {
      File homeDir = new File(System.getProperty("user.home"));
      return new File(homeDir, localDev.substring(2)).exists();
    }
    return new File(localDev).exists();
  }
}
