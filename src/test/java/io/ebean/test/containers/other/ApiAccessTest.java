package io.ebean.test.containers.other;

import io.ebean.test.containers.PostgresContainer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ApiAccessTest {

  /**
   * Check that using Builder we can only access setters and build() method.
   */
  @Disabled
  @Test
  void test() {
      PostgresContainer container = PostgresContainer.builder("15")
        .containerName("temp_pg15_9826")
        .port(9826)
        .build();

      container.start();
  }
}
