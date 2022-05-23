package io.ebean.test.containers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DockerHostTest {

  @Test
  void runningInDocker_when_false_alwaysUseLocalhost() {
    DockerHost dockerHost = new DockerHost();
    assertFalse(dockerHost.runningInDocker());
    assertEquals("localhost", dockerHost.dockerHost());
  }

  @Disabled
  @Test
  void runningInDocker_when_trueAndLinux_useDefault() {
    TD_InDockerHost dockerHost = new TD_InDockerHost();
    assertTrue(dockerHost.runningInDocker());

    assertEquals("172.17.0.1", dockerHost.dockerHost());
  }

  @Test
  void runningInDocker_when_windowsDefault() {
    String origName = System.getProperty("os.name");
    System.setProperty("os.name", "win");
    try {
      TD_InDockerHost dockerHost = new TD_InDockerHost();
      assertTrue(dockerHost.runningInDocker());
      assertEquals("host.docker.internal", dockerHost.dockerHost());
    } finally {
      System.setProperty("os.name", origName);
    }
  }

  @Test
  void runningInDocker_when_macDefault() {
    String origName = System.getProperty("os.name");
    System.setProperty("os.name", "mac");
    try {
      TD_InDockerHost dockerHost = new TD_InDockerHost();
      assertTrue(dockerHost.runningInDocker());
      assertEquals("host.docker.internal", dockerHost.dockerHost());
    } finally {
      System.setProperty("os.name", origName);
    }
  }


  @Test
  void runningInDocker_when_linuxDefault() {
    String origName = System.getProperty("os.name");
    System.setProperty("os.name", "linux");
    try {
      TD_InDockerHost dockerHost = new TD_InDockerHost();
      assertTrue(dockerHost.runningInDocker());
      assertEquals("172.17.0.1", dockerHost.dockerHost());
    } finally {
      System.setProperty("os.name", origName);
    }
  }

  static class TD_InDockerHost extends DockerHost {

    @Override
    boolean initInDocker() {
      return true;
    }
  }
}
