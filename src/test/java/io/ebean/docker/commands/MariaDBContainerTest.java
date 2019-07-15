package io.ebean.docker.commands;

import io.ebean.docker.container.ContainerConfig;
import io.ebean.docker.container.ContainerFactory;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public class MariaDBContainerTest {

  @Test
  public void start() {

    MariaDBConfig config = new MariaDBConfig("latest");
    config.setContainerName("temp_mariadb");
    config.setPort("8306");
    config.setFastStartMode(true);

    MariaDBContainer container = new MariaDBContainer(config);

    container.startWithCreate();
    container.startContainerOnly();
    container.startWithDropCreate();

    container.stopRemove();
  }

  @Ignore
  @Test
  public void viaContainerFactory() {

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
