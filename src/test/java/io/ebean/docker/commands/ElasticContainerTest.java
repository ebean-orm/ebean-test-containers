package io.ebean.docker.commands;

import org.junit.jupiter.api.Test;

class ElasticContainerTest {

  @Test
  void runProcess() {
    ElasticContainer elastic = ElasticContainer.builder("5.6.0").build();

    elastic.start();
    elastic.stop();
  }

}
