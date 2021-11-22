package io.ebean.docker.commands;

import org.junit.jupiter.api.Test;

import java.util.Properties;

public class ElasticContainerTest {

  @Test
  public void runProcess() {

    ElasticConfig config = new ElasticConfig("5.6.0", new Properties());

    ElasticContainer elastic = new ElasticContainer(config);
    elastic.start();
    elastic.stop();
  }

}
