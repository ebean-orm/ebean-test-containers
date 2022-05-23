package io.ebean.test.containers;

import io.ebean.test.containers.AutoStart;
import org.junit.jupiter.api.Test;

class AutoStartTest {

  @Test
  void test() {
    AutoStart.run();
    AutoStart.stop();
  }
}
