package io.ebean.docker.commands;

import org.junit.Test;

public class ClickHouseContainerTest {

  @Test
  public void runProcess() {

    ClickHouseConfig config = new ClickHouseConfig("latest");
    config.setStartMode("dropCreate");

    ClickHouseContainer container = new ClickHouseContainer(config);
    container.start();
    container.stopRemove();
  }
}
