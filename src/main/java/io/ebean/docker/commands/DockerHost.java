package io.ebean.docker.commands;

import java.io.File;
import java.util.Locale;

/**
 * Helper to detect if running inside docker and determine host name for that case.
 */
public class DockerHost {

  private final boolean runningInDocker = initInDocker();

  private final String dockerHost = initDockerHost();

  /**
   * Return true if running Docker-In-Docker.
   */
  boolean runningInDocker() {
    return runningInDocker;
  }

  /**
   * Return the default docker host to use taking into account Docker-In-Docker.
   */
  String dockerHost() {
    return dockerHost;
  }

  String initDockerHost() {
    return !runningInDocker ? "localhost" : dockerInDockerHost();
  }

  /**
   * Return true if running inside a docker container (we are using docker in docker).
   */
  boolean initInDocker() {
    return new File("/.dockerenv").exists();
  }

  /**
   * Return the default host name to use when running docker in docker.
   */
  String dockerInDockerHost() {
    String os = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
    if (os.contains("mac") || os.contains("darwin") || os.contains("win")) {
      return "host.docker.internal";
    } else {
      return "172.17.0.1";
    }
  }

}
