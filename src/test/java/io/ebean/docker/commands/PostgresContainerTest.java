package io.ebean.docker.commands;

import io.ebean.docker.container.ContainerConfig;
import io.ebean.docker.container.ContainerFactory;
import io.ebean.docker.container.Container;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PostgresContainerTest {

  @Test
  public void start() throws SQLException {

    PostgresConfig config = new PostgresConfig("10.1");
    config.setContainerName("junk_postgres10");
    config.setPort("9823");
    config.setExtensions(" hstore, , pgcrypto ");
    config.setInMemory(true);
//    config.setFastStartMode(true);
    config.setUser("main_user");
    config.setDbName("main_db");
    config.setInitSqlFile("init-main-database.sql");
    config.setSeedSqlFile("seed-main-database.sql");

//    config.setExtraDbUser("extra_user");
    config.setExtraDb("extra");
    config.setExtraDbInitSqlFile("init-extra-database.sql");
    config.setExtraDbSeedSqlFile("seed-extra-database.sql");

    PostgresContainer container = new PostgresContainer(config);

    container.stopRemove();
    container.startWithCreate();
    container.startContainerOnly();
    container.startWithDropCreate();

    assertTrue(container.isRunning());
    container.registerShutdownHook("stop");

    try (Connection connection = container.createConnection()) {
      exeSql(connection, "drop table if exists test_doesnotexist");
    }

    final String url = container.jdbcUrl();
    assertEquals(url, "jdbc:postgresql://localhost:9823/main_db");
    //container.stopOnly();
  }

  @Test
  public void viaContainerFactory() {

    Properties properties = new Properties();
    properties.setProperty("postgres.version", "10.1");
    properties.setProperty("postgres.containerName", "junk_postgres10");
    properties.setProperty("postgres.port", "9823");

    properties.setProperty("postgres.extensions", "hstore,pgcrypto");

    properties.setProperty("postgres.dbName", "test_roberto");
    properties.setProperty("postgres.username", "test_robino");

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
