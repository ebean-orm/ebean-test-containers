package io.ebean.docker.commands;

import io.ebean.docker.container.StartMode;
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
