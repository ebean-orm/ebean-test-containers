package io.ebean.test.containers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

class MySqlContainerTest {

  static final String MYSQL_VER = "8.0";

  @Disabled
  @Test
  void start_when_explicitCollation() {
    MySqlContainer container = MySqlContainer.builder(MYSQL_VER)
      .containerName("temp_mysql")
      .port(7306)
      .characterSet("utf8mb4")
      .collation("utf8mb4_unicode_ci")
      .build();

    container.startWithCreate();
    container.stopRemove();
  }

  @Disabled
  @Test
  void start_when_noCollation() {
    MySqlContainer container = MySqlContainer.builder(MYSQL_VER)
      .containerName("temp_mysql")
      .port(7306)
      .build();

    container.startWithCreate();
    container.stopRemove();
  }

  @Disabled
  @Test
  void start_when_defaultCollation() {
    MySqlContainer container = MySqlContainer.builder(MYSQL_VER)
      .containerName("temp_mysql")
      .port(7306)
      .collation("default")
      .build();

    container.startWithCreate();
    container.stopRemove();
  }

  @Test
  void start() {
    MySqlContainer container = MySqlContainer.builder(MYSQL_VER)
      .containerName("temp_mysql")
      .port(7306)
      .fastStartMode(true)
      .build();

    container.startWithCreate();
    container.startContainerOnly();
    container.startWithDropCreate();

    container.stopRemove();
  }

  @Test
  void viaContainerFactory() {
    Properties properties = new Properties();
    properties.setProperty("mysql.version", MYSQL_VER);
    properties.setProperty("mysql.containerName", "temp_mysql");
    properties.setProperty("mysql.port", "7306");

    properties.setProperty("mysql.name", "test_roberto");
    properties.setProperty("mysql.user", "test_robino");

    ContainerFactory factory = new ContainerFactory(properties);

    factory.startContainers(s -> System.out.println(">> " + s));

    ContainerConfig config = factory.config("mysql");

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
      st.execute();
    }
  }
}
