package io.ebean.docker.commands;

import io.ebean.docker.container.ContainerConfig;
import io.ebean.docker.container.ContainerFactory;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public class MySqlContainerTest {

  static final String MYSQL_VER = "8.0";

  @Ignore
  @Test
  public void start_when_explicitCollation() {

    MySqlConfig config = new MySqlConfig(MYSQL_VER);
    config.setContainerName("temp_mysql");
    config.setPort("7306");
    config.setCharacterSet("utf8mb4");
    config.setCollation("utf8mb4_unicode_ci");

    MySqlContainer container = new MySqlContainer(config);

    container.startWithCreate();
    container.stopRemove();
  }

  @Ignore
  @Test
  public void start_when_noCollation() {

    MySqlConfig config = new MySqlConfig(MYSQL_VER);
    config.setContainerName("temp_mysql");
    config.setPort("7306");

    MySqlContainer container = new MySqlContainer(config);

    container.startWithCreate();
    container.stopRemove();
  }

  @Ignore
  @Test
  public void start_when_defaultCollation() {

    MySqlConfig config = new MySqlConfig(MYSQL_VER);
    config.setContainerName("temp_mysql");
    config.setPort("7306");
    config.setCollation("default");

    MySqlContainer container = new MySqlContainer(config);

    container.startWithCreate();
    container.stopRemove();
  }

  @Test
  public void start() {

    MySqlConfig config = new MySqlConfig(MYSQL_VER);
    config.setContainerName("temp_mysql");
    config.setPort("7306");
    config.setFastStartMode(true);

    MySqlContainer container = new MySqlContainer(config);

    container.startWithCreate();
    container.startContainerOnly();
    container.startWithDropCreate();

    container.stopRemove();
  }

  @Test
  public void viaContainerFactory() {

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
