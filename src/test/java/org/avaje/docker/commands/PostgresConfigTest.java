package org.avaje.docker.commands;

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
    assertEquals(config.getStopMode(), "remove");
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
    assertEquals(config.getDbUser(), "test_user");
    assertEquals(config.getDbPassword(), "test");
    assertEquals(config.getDbAdminPassword(), "admin");
    assertFalse(config.isInMemory());
  }

  @Test
  public void properties_basic() {

    Properties properties = new Properties();
    properties.setProperty("postgres.version", "9.6");
    properties.setProperty("postgres.containerName", "junk_postgres");
    properties.setProperty("postgres.port", "9823");
    properties.setProperty("postgres.dbName", "baz");
    properties.setProperty("postgres.dbUser", "foo");
    properties.setProperty("postgres.dbPassword", "bar");
    properties.setProperty("postgres.dbAdminPassword", "bat");

    PostgresConfig config = new PostgresConfig("9.6", properties);
    assertEquals(config.containerName(), "junk_postgres");
    assertEquals(config.getPort(), "9823");
    assertEquals(config.getImage(), "postgres:9.6");
    assertEquals(config.getDbName(), "baz");
    assertEquals(config.getDbUser(), "foo");
    assertEquals(config.getDbPassword(), "bar");
    assertEquals(config.getDbAdminPassword(), "bat");
    assertFalse(config.isInMemory());
  }

}
