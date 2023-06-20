package io.ebean.test.containers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticContainerTest {

  @Test
  void randomPort() {
    ElasticContainer container = ElasticContainer.builder("6.8.23")
      .port(0)
      .build();

    container.startMaybe();
    assertThat(container.endpointUrl()).contains(":" + container.port());
  }

  @Test
  void runProcess() {
    ElasticContainer elastic = ElasticContainer.builder("6.8.23").build();

    elastic.startMaybe();
    elastic.stop();
  }

}
