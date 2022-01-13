package io.ebean.docker.commands;

import io.ebean.docker.container.Container;
import io.ebean.docker.container.ContainerConfig;
import io.ebean.docker.container.ContainerFactory;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PostgresContainerTest {

  @Test
  public void startPortBased() {
    PostgresConfig config = new PostgresConfig("13");
    config.setContainerName("junk_postgres13");
    config.setPort(9823);

    PostgresContainer dummy = new PostgresContainer(config);

    dummy.stopRemove();
    dummy.startContainerOnly();

    runBasedOnPort(9823);

    dummy.stopRemove();
  }

  private void runBasedOnPort(int port) {
    System.out.println("runBasedOnPort ... will connect and not start docker container");
    PostgresConfig config = new PostgresConfig("12");
    config.setContainerName("not_started");
    config.setPort(port);
    config.setExtensions("hstore,uuid-ossp");
    config.setStopMode(StopMode.Remove);

    PostgresContainer dummy = new PostgresContainer(config);

    dummy.start();

    try {
      Connection connection = config.createConnection();
      exeSql(connection, "create table test_junk2 (acol integer, map hstore)");
      exeSql(connection, "insert into test_junk2 (acol) values (42)");
      exeSql(connection, "insert into test_junk2 (acol) values (43)");
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void start() throws SQLException {

    PostgresConfig config = new PostgresConfig("13");
    config.setContainerName("junk_postgres13");
    config.setPort(9823);
    config.setExtensions(" hstore, , pgcrypto ");
    config.setInMemory(true);
    config.setUser("main_user");
    config.setDbName("main_db");
    config.setInitSqlFile("init-main-database.sql");
    config.setSeedSqlFile("seed-main-database.sql");

    config.setExtraDb("extra");
    config.setExtraDbInitSqlFile("init-extra-database.sql");
    config.setExtraDbSeedSqlFile("seed-extra-database.sql");

    PostgresContainer container = new PostgresContainer(config);

    container.stopRemove();
    container.startWithCreate();
    container.startContainerOnly();
    container.startWithDropCreate();

    assertTrue(container.isRunning());
    config.setShutdownMode(StopMode.Stop);
    container.registerShutdownHook();

    try (Connection connection = container.createConnection()) {
      exeSql(connection, "drop table if exists test_doesnotexist");
    }

    final String url = container.jdbcUrl();
    assertEquals(url, "jdbc:postgresql://localhost:9823/main_db");
//    container.stopRemove();
  }

  @Test
  public void viaContainerFactory() {

    Properties properties = new Properties();
    properties.setProperty("postgres.version", "13");
    properties.setProperty("postgres.containerName", "junk_postgres13");
    properties.setProperty("postgres.host", "127.0.0.1");
    properties.setProperty("postgres.port", "9823");

    properties.setProperty("postgres.extensions", "hstore,pgcrypto");

    properties.setProperty("postgres.dbName", "test_roberto");
    properties.setProperty("postgres.username", "test_robino");

    ContainerFactory factory = new ContainerFactory(properties);
    //factory.startContainers();

    Container container = factory.container("postgres");
    ContainerConfig config = container.config();
    assertEquals(9823, ((DbConfig) config).getPort());

    config.setStartMode(StartMode.DropCreate);
    container.start();

    config.setStartMode(StartMode.Container);
    container.start();

    config.setStartMode(StartMode.Create);
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
