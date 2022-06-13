package io.ebean.test.containers;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class PostgisContainerTest {

  @Test
  void extraDb() {
    PostgisContainer container = PostgisContainer.builder("14-3.2")
      .port(0)
      .extraDb("myextra")
      .build();

    container.start();
    assertThat(container.port()).isGreaterThan(0);

    ContainerConfig containerConfig = container.config();
    assertThat(containerConfig.port()).isEqualTo(container.port());

    String jdbcUrl = container.config().jdbcUrl();
    assertThat(jdbcUrl).contains(":" + containerConfig.port());
    runSomeSql(container);
  }

  private void runSomeSql(PostgisContainer container) {
    try {
      Connection connection = container.createConnection();
      exeSql(connection, "create table if not exists test_junk2 (acol integer, map hstore, mypoint geometry(point, 4326))");
      exeSql(connection, "insert into test_junk2 (acol) values (42)");
      exeSql(connection, "insert into test_junk2 (acol) values (43)");
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private void exeSql(Connection connection, String sql) throws SQLException {
    PreparedStatement st = connection.prepareStatement(sql);
    st.execute();
    st.close();
  }
}
