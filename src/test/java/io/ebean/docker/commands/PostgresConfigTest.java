package io.ebean.docker.commands;

import io.ebean.docker.container.StartMode;
import io.ebean.docker.container.StopMode;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class PostgresConfigTest {

  @Test
  void stopMode_global() {
    Properties properties = new Properties();
    properties.setProperty("stopMode", "remove");

    InternalConfigDb config = PostgresContainer.newBuilder("11").properties(properties).internalConfig();
    assertEquals(config.getStopMode(), StopMode.Remove);
  }

  @Test
  void stopMode_defaultStop() {
    Properties properties = new Properties();

    InternalConfigDb config = PostgresContainer.newBuilder("11").properties(properties).internalConfig();
    assertEquals(config.getStopMode(), StopMode.Remove);
  }

  @Test
  void stopMode_explicitlySet() {
    Properties properties = new Properties();
    properties.setProperty("stopMode", "remove");
    properties.setProperty("postgres.stopMode", "none");

    InternalConfigDb config = PostgresContainer.newBuilder("11").properties(properties).internalConfig();
    assertEquals(config.getStopMode(), StopMode.None);
  }


  @Test
  void properties_default() {
    Properties properties = new Properties();

    InternalConfigDb config = PostgresContainer.newBuilder("11").properties(properties).internalConfig();
    assertEquals(config.containerName(), "ut_postgres");
    assertEquals(config.getPort(), 6432);
    assertEquals(config.getHost(), "localhost");
    assertEquals(config.getImage(), "postgres:11");
    assertEquals(config.getDbName(), "test_db");
    assertEquals(config.getUsername(), "test_user");
    assertEquals(config.getPassword(), "test");
    assertEquals(config.getAdminUsername(), "postgres");
    assertEquals(config.getAdminPassword(), "admin");
    assertEquals(config.getStartMode(), StartMode.Create);
    assertEquals(config.getStopMode(), StopMode.Remove);
    assertEquals(config.shutdownMode(), StopMode.None);
    assertTrue(config.isFastStartMode());
    assertFalse(config.isInMemory());
  }

  @Test
  void properties_basic() {
    Properties properties = new Properties();
    properties.setProperty("postgres.version", "11");
    properties.setProperty("postgres.containerName", "junk_postgres");
    properties.setProperty("postgres.host", "172.17.0.1"); // e.g. running in Docker (linux)
    properties.setProperty("postgres.port", "9823");
    properties.setProperty("postgres.dbName", "baz");
    properties.setProperty("postgres.username", "foo");
    properties.setProperty("postgres.password", "bar");
    properties.setProperty("postgres.adminPassword", "bat");
    properties.setProperty("postgres.fastStartMode", "false");

    properties.setProperty("postgres.initSqlFile", "init.sql");
    properties.setProperty("postgres.seedSqlFile", "seed.sql");
    properties.setProperty("postgres.extraDb.initSqlFile", "extra_init.sql");
    properties.setProperty("postgres.extraDb.seedSqlFile", "extra_seed.sql");

    InternalConfigDb config = PostgresContainer.newBuilder("11").properties(properties).internalConfig();
    assertEquals(config.containerName(), "junk_postgres");
    assertEquals(config.getPort(), 9823);
    assertEquals(config.getHost(), "172.17.0.1");
    assertEquals(config.getImage(), "postgres:11");
    assertEquals(config.getDbName(), "baz");
    assertEquals(config.getUsername(), "foo");
    assertEquals(config.getPassword(), "bar");
    assertEquals(config.getAdminPassword(), "bat");

    assertEquals(config.getInitSqlFile(), "init.sql");
    assertEquals(config.getSeedSqlFile(), "seed.sql");
    assertEquals(config.getExtraDbInitSqlFile(), "extra_init.sql");
    assertEquals(config.getExtraDbSeedSqlFile(), "extra_seed.sql");

    assertEquals(config.jdbcAdminUrl(), "jdbc:postgresql://172.17.0.1:9823/postgres");
    assertEquals(config.jdbcUrl(), "jdbc:postgresql://172.17.0.1:9823/baz");
    assertFalse(config.isFastStartMode());
    assertFalse(config.isInMemory());
  }

}
