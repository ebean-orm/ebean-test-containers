package io.ebean.docker.commands;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class PostgresConfigTest {

  @Test
  public void stopMode_global() {

    Properties properties = new Properties();
    properties.setProperty("stopMode", "remove");

    PostgresConfig config = new PostgresConfig("9.6", properties);
    assertEquals(config.getStopMode(), "remove");
  }

  @Test
  public void stopMode_defaultStop() {

    Properties properties = new Properties();

    PostgresConfig config = new PostgresConfig("9.6", properties);
    assertEquals(config.getStopMode(), "stop");
  }

  @Test
  public void stopMode_explicitlySet() {

    Properties properties = new Properties();
    properties.setProperty("stopMode", "remove");
    properties.setProperty("postgres.stopMode", "none");

    PostgresConfig config = new PostgresConfig("9.6", properties);
    assertEquals(config.getStopMode(), "none");
  }


  @Test
  public void properties_default() {

    Properties properties = new Properties();

    PostgresConfig config = new PostgresConfig("9.6", properties);
    assertEquals(config.containerName(), "ut_postgres");
    assertEquals(config.getPort(), "6432");
    assertEquals(config.getImage(), "postgres:9.6");
    assertEquals(config.getDbName(), "test_db");
    assertEquals(config.getUsername(), "test_user");
    assertEquals(config.getPassword(), "test");
    assertEquals(config.getAdminPassword(), "admin");
    assertFalse(config.isInMemory());
  }

  @Test
  public void properties_basic() {

    Properties properties = new Properties();
    properties.setProperty("postgres.version", "9.6");
    properties.setProperty("postgres.containerName", "junk_postgres");
    properties.setProperty("postgres.port", "9823");
    properties.setProperty("postgres.dbName", "baz");
    properties.setProperty("postgres.username", "foo");
    properties.setProperty("postgres.password", "bar");
    properties.setProperty("postgres.adminPassword", "bat");

    PostgresConfig config = new PostgresConfig("9.6", properties);
    assertEquals(config.containerName(), "junk_postgres");
    assertEquals(config.getPort(), "9823");
    assertEquals(config.getImage(), "postgres:9.6");
    assertEquals(config.getDbName(), "baz");
    assertEquals(config.getUsername(), "foo");
    assertEquals(config.getPassword(), "bar");
    assertEquals(config.getAdminPassword(), "bat");
    assertFalse(config.isInMemory());
  }

}
