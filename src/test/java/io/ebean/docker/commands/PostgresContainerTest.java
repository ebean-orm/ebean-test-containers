package io.ebean.docker.commands;

import io.ebean.docker.container.Container;
import io.ebean.docker.container.ContainerConfig;
import io.ebean.docker.container.ContainerFactory;
import io.ebean.docker.container.StopMode;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresContainerTest {

  @Test
  void startPortBased() {
    PostgresConfig config = new PostgresConfig("14");
    config.containerName("temp_postgres14");
    config.port(9823);

    PostgresContainer dummy = new PostgresContainer(config);

    dummy.stopRemove();
    dummy.startContainerOnly();

    runBasedOnPort(9823);

    dummy.stopRemove();
  }

  private void runBasedOnPort(int port) {
    System.out.println("runBasedOnPort ... will connect and not start docker container");
    PostgresConfig config = new PostgresConfig("14");
    config.containerName("not_started");
    config.port(port);
    config.extensions("hstore,uuid-ossp");
    config.stopMode(StopMode.Remove);

    PostgresContainer container = new PostgresContainer(config);
    container.start();

    try {
      Connection connection = container.createConnection();
      exeSql(connection, "create table test_junk2 (acol integer, map hstore)");
      exeSql(connection, "insert into test_junk2 (acol) values (42)");
      exeSql(connection, "insert into test_junk2 (acol) values (43)");
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void start() throws SQLException {
    PostgresConfig config = new PostgresConfig("14");
    config.containerName("temp_postgres14");
    config.port(9823);
    config.extensions(" hstore, , pgcrypto ");
    config.inMemory(true);
    config.user("main_user");
    config.dbName("main_db");
    config.initSqlFile("init-main-database.sql");
    config.seedSqlFile("seed-main-database.sql");

    config.extraDb("extra");
    config.extraDbInitSqlFile("init-extra-database.sql");
    config.extraDbSeedSqlFile("seed-extra-database.sql");

    PostgresContainer container = new PostgresContainer(config);

    container.stopRemove();
    container.startWithCreate();
    container.startContainerOnly();
    container.startWithDropCreate();

    assertTrue(container.isRunning());
    //config.setShutdownMode(StopMode.Stop);
    //container.registerShutdownHook();

    try (Connection connection = container.createConnection()) {
      exeSql(connection, "drop table if exists test_doesnotexist");
    }

    final String url = container.jdbcUrl();
    assertEquals(url, "jdbc:postgresql://localhost:9823/main_db");
    container.stopRemove();
  }

  @Test
  void viaContainerFactory() {
    Properties properties = new Properties();
    properties.setProperty("postgres.version", "14");
    properties.setProperty("postgres.containerName", "temp_postgres14");
    properties.setProperty("postgres.host", "127.0.0.1");
    properties.setProperty("postgres.port", "9823");

    properties.setProperty("postgres.extensions", "hstore,pgcrypto");

    properties.setProperty("postgres.dbName", "test_roberto");
    properties.setProperty("postgres.username", "test_robino");
    properties.setProperty("postgres.startMode", "dropCreate");

    ContainerFactory factory = new ContainerFactory(properties);
    //factory.startContainers();

    Container container = factory.container("postgres");
    ContainerConfig config = container.config();
    // assertEquals(9823, config.port());
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
