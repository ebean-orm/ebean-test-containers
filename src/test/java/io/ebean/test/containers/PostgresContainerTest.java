package io.ebean.test.containers;

import io.avaje.applog.AppLog;
import io.ebean.Database;
import io.ebean.datasource.DataSourcePool;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import static java.lang.System.Logger.Level.INFO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresContainerTest {

  private final System.Logger log = AppLog.getLogger(PostgresContainerTest.class);

  @Test
  void extraDb() {
    PostgresContainer container = PostgresContainer.builder("15")
      .port(0)
      .extensions("hstore")
      .extraDb("myextra")
      .extraDbUser("extra1")
      .extraDbPassword("extraPwd")
      .extraDbExtensions("pgcrypto")
      .extraDbInitSqlFile("init-extra-database.sql")
      .extra2(e -> e.dbName("my_extra2")
        .username("my_extra2user")
        .initSqlFile("init-extra2-database.sql"))
      .build();

    container.startMaybe();
    assertThat(container.port()).isGreaterThan(0);

    ContainerConfig containerConfig = container.config();
    assertThat(containerConfig.port()).isEqualTo(container.port());

    String jdbcUrl = container.config().jdbcUrl();
    assertThat(jdbcUrl).contains(":" + containerConfig.port());
    runSomeSql(container);

    Database ebeanExtra = container.ebean().extraDatabaseBuilder().build();
    ebeanExtra.sqlUpdate("insert into foo_extra (acol) values (44)").execute();

    Database ebeanExtra2 = container.ebean().extra2DatabaseBuilder().build();
    ebeanExtra2.sqlUpdate("insert into bar_extra2 (acol) values (44)").execute();
  }

  @Test
  void randomPort() {
    PostgresContainer container = PostgresContainer.builder("15")
      .port(0)
      .extensions("hstore")
      .build();

    container.startMaybe();
    assertThat(container.port()).isGreaterThan(0);

    ContainerConfig containerConfig = container.config();
    assertThat(containerConfig.port()).isEqualTo(container.port());

    String jdbcUrl = container.config().jdbcUrl();
    assertThat(jdbcUrl).contains(":" + containerConfig.port());
    runSomeSql(container);
  }

  @Test
  void defaultPort() {
    PostgresContainer container = PostgresContainer.builder("15")
      .dbName("my_test")
      .extensions("hstore")
      .build()
      .start();

    container.startMaybe();
    runSomeSql(container);
  }

  @Test
  void startPortBased() {
    log.log(INFO, "startPortBased() start ...");
    PostgresContainer container = PostgresContainer.builder("15")
      .containerName("temp_pg15_9824")
      .port(9824)
      .build();

    container.stopRemove();
    Commands commands = new Commands();
    commands.stopContainers("temp_pg15_9824");
    commands.removeContainers("temp_pg15_9824");

    container.startContainerOnly();


    runBasedOnPort(9824);

    container.stopRemove();
    log.log(INFO, "startPortBased() finished");
  }

  private void runBasedOnPort(int port) {
    log.log(INFO, "runBasedOnPort ... will connect and not start docker container");
    PostgresContainer container = PostgresContainer.builder("15")
      .containerName("not_started")
      .port(port)
      .extensions("hstore,uuid-ossp")
      .build();

    container.startMaybe();

    runSomeSql(container);
    log.log(INFO, "runBasedOnPort done");
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
  void start() throws SQLException, InterruptedException {
    log.log(INFO, "start() ... ");
    PostgresContainer container = PostgresContainer.builder("15")
      .containerName("temp_postgres15")
      .port(9828)
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

    log.log(INFO, "start() ... stopRemove()");
    container.stopRemove();
    log.log(INFO, "start() ... .startWithCreate()");
    container.startWithCreate();
    container.startContainerOnly();
    container.startWithDropCreate();

    assertTrue(container.isRunning());
    //config.setShutdownMode(StopMode.Stop);
    //container.registerShutdownHook();

    try (Connection connection = container.createConnection()) {
      exeSql(connection, "drop table if exists test_doesnotexist");
    }

    assertEquals("jdbc:postgresql://localhost:9828/main_db", container.jdbcUrl());

    Database ebean = container.ebean().builder().build();
    ebean.sqlUpdate("insert into foo_main (mcol) values (?)")
      .setParameter(2)
      .execute();

    Database extraEbean = container.ebean().extraDatabaseBuilder().build();
    extraEbean.sqlUpdate("insert into foo_extra (acol) values (?)")
      .setParameter(2)
      .execute();

    DataSourcePool dataSource = container.ebean().dataSourceBuilder().build();
    try (Connection connection = dataSource.getConnection()) {
      exeSql(connection, "insert into foo_main (mcol) values (1)");
    }
    dataSource.shutdown();

    DataSourcePool extraDataSource = container.ebean().extraDataSourceBuilder().build();
    try (Connection connection = extraDataSource.getConnection()) {
      exeSql(connection, "insert into foo_extra (acol) values (1)");
    }
    extraDataSource.shutdown();

    container.stopRemove();
    log.log(INFO, "start() finished");
  }

  @Test
  void viaContainerFactory() {
    Properties properties = new Properties();
    properties.setProperty("postgres.version", "15");
    properties.setProperty("postgres.containerName", "temp_pg15_b");
    properties.setProperty("postgres.host", "127.0.0.1");
    properties.setProperty("postgres.port", "9825");

    properties.setProperty("postgres.extensions", "hstore,pgcrypto");

    properties.setProperty("postgres.dbName", "test_roberto");
    properties.setProperty("postgres.username", "test_robino");
    properties.setProperty("postgres.startMode", "dropCreate");

    ContainerFactory factory = new ContainerFactory(properties);
    //factory.startContainers();

    Container container = factory.container("postgres");
    ContainerConfig config = container.config();
    container.startMaybe();

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
