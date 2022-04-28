package io.ebean.docker.other;

import io.ebean.docker.commands.PostgresContainer;
import org.junit.jupiter.api.Test;

class ApiAccessTest {

  /**
   * Check that using Builder we can only access setters and build() method.
   */
  @Test
  void test() {
      PostgresContainer container = PostgresContainer.newBuilder("14")
        .setContainerName("temp_postgres14")
        .setPort(9823)
        .build();

      container.start();
  }
}
