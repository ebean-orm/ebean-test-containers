package io.ebean.test.containers;

import org.junit.jupiter.api.Test;

class ClickHouseContainerTest {

  @Test
  void runProcess() {
    ClickHouseContainer container = ClickHouseContainer.builder("latest")
      .startMode(StartMode.DropCreate)
      .build();

    container.start();
    container.stopRemove();
  }
}
