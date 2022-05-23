package io.ebean.test.containers;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresContainerTest {

  @Test
  void randomPort() {
    PostgresContainer container = PostgresContainer.builder("14")
      .port(0)
      .extensions("hstore")
      .build();

    container.start();
    assertThat(container.port()).isGreaterThan(0);

    ContainerConfig containerConfig = container.config();
    assertThat(containerConfig.port()).isEqualTo(container.port());

    String jdbcUrl = container.config().jdbcUrl();
    assertThat(jdbcUrl).contains(":" + containerConfig.port());
    runSomeSql(container);
  }

  @Test
  void defaultPort() {
    PostgresContainer container = PostgresContainer.builder("14")
      .extensions("hstore")
      .build();

    container.start();
    runSomeSql(container);
  }

  @Test
  void startPortBased() {
    PostgresContainer container = PostgresContainer.builder("14")
      .containerName("temp_postgres14")
      .port(9823)
      .build();

    container.stopRemove();
    container.startContainerOnly();

    runBasedOnPort(9823);

    container.stopRemove();
  }

  private void runBasedOnPort(int port) {
    System.out.println("runBasedOnPort ... will connect and not start docker container");
    PostgresContainer container = PostgresContainer.builder("14")
      .containerName("not_started")
      .port(port)
      .extensions("hstore,uuid-ossp")
      .build();

    container.start();

    runSomeSql(container);
  }

  private void runSomeSql(PostgresContainer container) {
    try {
      Connection connection = container.createConnection();
      exeSql(connection, "create table if not exists test_junk2 (acol integer, map hstore)");
      exeSql(connection, "insert into test_junk2 (acol) values (42)");
      exeSql(connection, "insert into test_junk2 (acol) values (43)");
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void start() throws SQLException {
    PostgresContainer container = PostgresContainer.builder("14")
      .containerName("temp_postgres14")
      .port(9823)
      .extensions(" hstore, , pgcrypto ")
      .inMemory(true)
      .user("main_user")
      .dbName("main_db")
      .initSqlFile("init-main-database.sql")
      .seedSqlFile("seed-main-database.sql")
      .extraDb("extra")
      .extraDbInitSqlFile("init-extra-database.sql")
      .extraDbSeedSqlFile("seed-extra-database.sql")
      .build();

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
