package io.ebean.docker.container;

import org.junit.jupiter.api.Test;

public class AutoStartTest {

  @Test
  public void test() {
    AutoStart.run();
    AutoStart.stop();
  }
}
