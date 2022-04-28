package io.ebean.docker.commands;

import io.ebean.docker.container.ContainerConfig;

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

  boolean checkSkipStop();

  String docker();

  String image();
}
