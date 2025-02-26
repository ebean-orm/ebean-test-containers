package io.ebean.test.containers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class MariaDBContainerTest {

  @Test
  void randomPort() {
    MariaDBContainer container = MariaDBContainer.builder("latest")
      .port(0)
      .build();

    container.startMaybe();

    String jdbcUrl = container.jdbcUrl();
    assertThat(jdbcUrl).contains(":" + container.port());
    try (Connection connection = container.createConnection()) {
      exeSql(connection, "drop table if exists maria_random");
      exeSql(connection, "create table maria_random (acol integer)");
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Test
  void start() {
    MariaDBContainer container = MariaDBContainer.builder("latest")
      .containerName("temp_mariadb")
      .extraDb("foo, bar")
      .port(8306)
      .fastStartMode(true)
      .build();

    container.startWithCreate();
    container.startContainerOnly();
    container.startWithDropCreate();

    container.stopRemove();
  }

  @Disabled
  @Test
  void viaContainerFactory() {
    Properties properties = new Properties();
    properties.setProperty("mariadb.version", "10.4");
    properties.setProperty("mariadb.containerName", "temp_mariadb");
    properties.setProperty("mariadb.port", "8306");

    properties.setProperty("mariadb.name", "test_roberto");
    properties.setProperty("mariadb.user", "test_robino");

    ContainerFactory factory = new ContainerFactory(properties);

    factory.startContainers(s -> System.out.println(">> " + s));

    ContainerConfig config = factory.config("mariadb");

    try {
      Connection connection = config.createConnection();

      exeSql(connection, "drop table if exists test_junk2");
      exeSql(connection, "create table test_junk2 (acol integer)");
      exeSql(connection, "insert into test_junk2 (acol) values (42)");
      exeSql(connection, "insert into test_junk2 (acol) values (43)");

      connection.close();

    } catch (SQLException e) {
      throw new RuntimeException(e);

    } finally {
      factory.stopContainers(s -> System.out.println(">> " + s));
    }
  }

  private void exeSql(Connection connection, String sql) throws SQLException {
    try (PreparedStatement st = connection.prepareStatement(sql)) {
      System.out.println("executed " + sql);
      st.execute();
    }
  }
}
