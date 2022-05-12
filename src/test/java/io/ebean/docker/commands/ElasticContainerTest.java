package io.ebean.docker.commands;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticContainerTest {

  @Test
  void randomPort() {
    ElasticContainer container = ElasticContainer.builder("5.6.0")
      .port(0)
      .build();

    container.start();
    assertThat(container.endpointUrl()).contains(":" + container.port());
  }

  @Test
  void runProcess() {
    ElasticContainer elastic = ElasticContainer.builder("5.6.0").build();

    elastic.start();
    elastic.stop();
  }

}
