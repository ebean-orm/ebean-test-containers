package io.ebean.docker.container;

import org.junit.jupiter.api.Test;

class AutoStartTest {

  @Test
  void test() {
    AutoStart.run();
    AutoStart.stop();
  }
}
