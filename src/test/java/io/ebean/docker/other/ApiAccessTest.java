package io.ebean.docker.other;

import io.ebean.docker.commands.PostgresContainer;
import org.junit.jupiter.api.Test;

class ApiAccessTest {

  /**
   * Check that using Builder we can only access setters and build() method.
   */
  @Test
  void test() {
      PostgresContainer container = PostgresContainer.builder("14")
        .containerName("temp_postgres14")
        .port(9823)
        .build();

      container.start();
  }
}
