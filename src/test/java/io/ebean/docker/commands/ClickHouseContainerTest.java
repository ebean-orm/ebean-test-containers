package io.ebean.docker.commands;

import org.junit.jupiter.api.Test;

public class ClickHouseContainerTest {

  @Test
  public void runProcess() {

    ClickHouseConfig config = new ClickHouseConfig("latest");
    config.setStartMode(StartMode.DropCreate);

    ClickHouseContainer container = new ClickHouseContainer(config);
    container.start();
    container.stopRemove();
  }
}
