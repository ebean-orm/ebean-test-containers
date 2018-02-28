package org.avaje.docker.commands;

import org.avaje.docker.container.Container;
import org.avaje.docker.container.ContainerConfig;
import org.avaje.docker.container.ContainerFactory;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public class PostgresContainerTest {

  @Test
  public void start() {

    PostgresConfig config = new PostgresConfig("10.1");
    config.setContainerName("junk_postgres10");
    config.setPort("9823");

    PostgresContainer container = new PostgresContainer(config);

//    container.stopRemove();
    container.startWithCreate();
    container.startContainerOnly();
    container.startWithDropCreate();

    //container.stopOnly();
  }

  @Test
  public void viaContainerFactory() {

    Properties properties = new Properties();
    properties.setProperty("postgres.version", "10.1");
    properties.setProperty("postgres.containerName", "junk_postgres10");
    properties.setProperty("postgres.port", "9823");

    properties.setProperty("postgres.dbExtensions", "hstore,pgcrypto");

    properties.setProperty("postgres.dbName", "test_roberto");
    properties.setProperty("postgres.dbUser", "test_robino");

    ContainerFactory factory = new ContainerFactory(properties);
    //factory.startContainers();

    Container container = factory.container("postgres");
    ContainerConfig config = container.config();

    config.setStartMode("dropCreate");
    container.start();

    config.setStartMode("container");
    container.start();

    config.setStartMode("create");
    container.start();

    //String url = config.jdbcUrl();
    //String url = "jdbc:postgresql://localhost:" + config.dbPort + "/" + config.dbName;

    try {
      Connection connection = config.createConnection();
      //Connection connection = DriverManager.getConnection(url, config.dbUser, config.dbPassword);

      exeSql(connection, "drop table if exists test_junk");
      exeSql(connection, "create table test_junk (acol integer, map hstore)");
      exeSql(connection, "insert into test_junk (acol) values (42)");
      exeSql(connection, "insert into test_junk (acol) values (43)");

    } catch (SQLException e) {
      throw new RuntimeException(e);

    } finally {
      container.stop();
    }
  }

  private void exeSql(Connection connection, String sql) throws SQLException {
    PreparedStatement st = connection.prepareStatement(sql);
    st.execute();
    st.close();
  }
}
