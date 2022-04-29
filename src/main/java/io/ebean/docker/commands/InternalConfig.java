package io.ebean.docker.commands;

import io.ebean.docker.container.ContainerConfig;
import io.ebean.docker.container.StartMode;
import io.ebean.docker.container.StopMode;

interface InternalConfig extends ContainerConfig {

  String getHost();

  int getPort();

  int getInternalPort();

  int getAdminPort();

  int getAdminInternalPort();

  String getImage();

  StartMode getStartMode();

  StopMode getStopMode();

  int getMaxReadyAttempts();

  String getDocker();

  StopMode shutdownMode();

  boolean isStopModeNone();

  /**
   * Return true if shutdown hook registration should be skipped by
   * the presence of ~/.ebean/ignore-docker-shutdown file.
   */
  boolean checkSkipShutdown();

  String docker();

  String image();
}
