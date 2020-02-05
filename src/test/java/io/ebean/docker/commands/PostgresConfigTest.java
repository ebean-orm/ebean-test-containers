package io.ebean.docker.commands;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PostgresConfigTest {

  @Test
  public void stopMode_global() {

    Properties properties = new Properties();
    properties.setProperty("stopMode", "remove");

    PostgresConfig config = new PostgresConfig("11", properties);
    assertEquals(config.getStopMode(), StopMode.Remove);
  }

  @Test
  public void stopMode_defaultStop() {

    Properties properties = new Properties();

    PostgresConfig config = new PostgresConfig("11", properties);
    assertEquals(config.getStopMode(), StopMode.Stop);
  }

  @Test
  public void stopMode_explicitlySet() {

    Properties properties = new Properties();
    properties.setProperty("stopMode", "remove");
    properties.setProperty("postgres.stopMode", "none");

    PostgresConfig config = new PostgresConfig("11", properties);
    assertEquals(config.getStopMode(), StopMode.None);
  }


  @Test
  public void properties_default() {

    Properties properties = new Properties();

    PostgresConfig config = new PostgresConfig("11", properties);
    assertEquals(config.containerName(), "ut_postgres");
    assertEquals(config.getPort(), 6432);
    assertEquals(config.getImage(), "postgres:11");
    assertEquals(config.getDbName(), "test_db");
    assertEquals(config.getUsername(), "test_user");
    assertEquals(config.getPassword(), "test");
    assertEquals(config.getAdminUsername(), "postgres");
    assertEquals(config.getAdminPassword(), "");
    assertEquals(config.getStartMode(), StartMode.Create);
    assertEquals(config.getStopMode(), StopMode.Stop);
    assertEquals(config.shutdownMode(), StopMode.None);
    assertTrue(config.isFastStartMode());
    assertFalse(config.isInMemory());
  }

  @Test
  public void properties_basic() {

    Properties properties = new Properties();
    properties.setProperty("postgres.version", "11");
    properties.setProperty("postgres.containerName", "junk_postgres");
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

    PostgresConfig config = new PostgresConfig("11", properties);
    assertEquals(config.containerName(), "junk_postgres");
    assertEquals(config.getPort(), 9823);
    assertEquals(config.getImage(), "postgres:11");
    assertEquals(config.getDbName(), "baz");
    assertEquals(config.getUsername(), "foo");
    assertEquals(config.getPassword(), "bar");
    assertEquals(config.getAdminPassword(), "bat");

    assertEquals(config.getInitSqlFile(), "init.sql");
    assertEquals(config.getSeedSqlFile(), "seed.sql");
    assertEquals(config.getExtraDbInitSqlFile(), "extra_init.sql");
    assertEquals(config.getExtraDbSeedSqlFile(), "extra_seed.sql");

    assertFalse(config.isFastStartMode());
    assertFalse(config.isInMemory());
  }

}
