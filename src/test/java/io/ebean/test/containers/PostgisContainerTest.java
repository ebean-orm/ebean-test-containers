package io.ebean.test.containers;

import io.ebean.Database;
import io.ebean.datasource.DataSourcePool;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class PostgisContainerTest {

  @Test
  void extraDb() throws java.sql.SQLException {
    PostgisContainer container = PostgisContainer.builder("15")
      .port(0)
      .useLW(true)
      .extraDb("myextra")
      .build();

    container.startMaybe();
    assertThat(container.port()).isGreaterThan(0);

    ContainerConfig containerConfig = container.config();
    assertThat(containerConfig.port()).isEqualTo(container.port());

    String jdbcUrl = container.config().jdbcUrl();
    assertThat(jdbcUrl).contains(":" + containerConfig.port());
    assertThat(jdbcUrl).startsWith("jdbc:postgresql_lwgis://");
    runSomeSql(container);

    DataSourcePool dataSource = container.ebean().dataSourceBuilder().build();
    try (Connection connection = dataSource.getConnection()) {
      exeSql(connection, "insert into test_junk2 (acol) values (44)");
    }
    dataSource.shutdown();

    Database ebean = container.ebean().builder()
      .register(false)
      .defaultDatabase(false)
      .build();
    ebean.sqlUpdate("insert into test_junk2 (acol) values (?)")
      .setParameter(45)
      .execute();

    ebean.shutdown();
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

  private static void exeSql(Connection connection, String sql) throws SQLException {
    try (PreparedStatement st = connection.prepareStatement(sql)) {
      st.execute();
    }
  }
}
