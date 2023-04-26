package io.ebean.test.containers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Properties;

import static org.assertj.core.api.Assumptions.assumeThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ContainerFactoryTest {

  static final String MYSQL_VER = "8.0";

  @Test
  void runWith() {
    Properties properties = new Properties();
    properties.setProperty("postgres.version", "9.6");
    properties.setProperty("mysql.version", MYSQL_VER);
    properties.setProperty("sqlserver.version", "2017-CE");
    properties.setProperty("postgis.version", "14-3.2");

    ContainerFactory factory = new ContainerFactory(properties);

    assertEquals("9.6", factory.runWithVersion("postgres"));
    assertEquals(MYSQL_VER, factory.runWithVersion("mysql"));
    assertEquals("2017-CE", factory.runWithVersion("sqlserver"));
    assertEquals("14-3.2", factory.runWithVersion("postgis"));
  }

  @Test
  void runWithNuoDB() {
    Properties properties = new Properties();
    properties.setProperty("nuodb.version", "4.0.0");
    properties.setProperty("nuodb.schema", "junk");
    properties.setProperty("nuodb.username", "junk");

    ContainerFactory factory = new ContainerFactory(properties);
    assertEquals("4.0.0", factory.runWithVersion("nuodb"));

    //factory.startContainers();
    //factory.stopContainers();
  }

  @Disabled
  @Test
  void runWith_Cockroach() {
    Properties properties = new Properties();
    properties.setProperty("cockroach.version", "v21.2.9");

    ContainerFactory factory = new ContainerFactory(properties);
    assertEquals("v21.2.9", factory.runWithVersion("cockroach"));

    factory.startContainers();
    factory.stopContainers();
  }

  @Test
  void runWith_Yugabyte() {
    Properties properties = new Properties();
    properties.setProperty("yugabyte.version", "2.11.2.0-b89");

    ContainerFactory factory = new ContainerFactory(properties);
    assertEquals("2.11.2.0-b89", factory.runWithVersion("yugabyte"));

    // factory.startContainers();
    // factory.stopContainers();
  }

  @Test
  void runWithHana() {
    assumeThat(System.getProperty("os.name").toLowerCase()).contains("linux");

    Properties properties = new Properties();
    properties.setProperty("postgres.version", "9.6");
    properties.setProperty("mysql.version", MYSQL_VER);
    properties.setProperty("sqlserver.version", "2017-CE");
    //properties.setProperty("hana.version", "2.00.033.00.20180925.2");

    ContainerFactory factory = new ContainerFactory(properties);

    assertEquals("9.6", factory.runWithVersion("postgres"));
    assertEquals(MYSQL_VER, factory.runWithVersion("mysql"));
    assertEquals("2017-CE", factory.runWithVersion("sqlserver"));
    //assertEquals("2.00.033.00.20180925.2", factory.runWithVersion("hana"));
  }

  @Test
  void runWith_specifiedWithComma() {
    Properties properties = new Properties();
    properties.setProperty("postgres.version", "9.6");
    properties.setProperty("mysql.version", MYSQL_VER);
    properties.setProperty("sqlserver.version", "2017-CE");
    properties.setProperty("hana.version", "2.00.033.00.20180925.2");

    ContainerFactory factory = new ContainerFactory(properties, "sqlserver,mysql");

    assertNull(factory.runWithVersion("postgres"));
    assertEquals(MYSQL_VER, factory.runWithVersion("mysql"));
    assertEquals("2017-CE", factory.runWithVersion("sqlserver"));
    assertNull(factory.runWithVersion("hana"));
  }

  @Test
  void runWith_specifiedOne() {
    Properties properties = new Properties();
    properties.setProperty("postgres.version", "9.6");
    properties.setProperty("mysql.version", MYSQL_VER);
    properties.setProperty("sqlserver.version", "2017-CE");
    properties.setProperty("hana.version", "2.00.033.00.20180925.2");

    ContainerFactory factory = new ContainerFactory(properties, "mysql");

    assertEquals(MYSQL_VER, factory.runWithVersion("mysql"));
    assertNull(factory.runWithVersion("postgres"));
    assertNull(factory.runWithVersion("sqlserver"));
    assertNull(factory.runWithVersion("hana"));
  }

  @Test
  void runWith_specified_viaEnv() {
    Properties properties = new Properties();
    properties.setProperty("postgres.version", "9.6");
    properties.setProperty("mysql.version", MYSQL_VER);
    properties.setProperty("sqlserver.version", "2017-CE");
    properties.setProperty("hana.version", "2.00.033.00.20180925.2");
    properties.setProperty("yugabyte.version", "2.8");

    System.setProperty("docker_run_with", "mysql");
    try {
      ContainerFactory factory = new ContainerFactory(properties);

      assertEquals(MYSQL_VER, factory.runWithVersion("mysql"));
      assertNull(factory.runWithVersion("postgres"));
      assertNull(factory.runWithVersion("sqlserver"));
      assertNull(factory.runWithVersion("hana"));
      assertNull(factory.runWithVersion("yugabyte"));

    } finally {
      System.clearProperty("docker_run_with");
    }
  }

  @Test
  void create() throws Exception {
    Properties properties = new Properties();
    properties.setProperty("postgres.version", "15");
    properties.setProperty("postgres.containerName", "junk_postgres15");
    properties.setProperty("postgres.port", "9823");

    properties.setProperty("elastic.version", "5.6.0");
    properties.setProperty("elastic.port", "9201");

    properties.setProperty("redis.version", "latest");
    properties.setProperty("redis.port", "9911");
    properties.setProperty("redis.containerName", "junk_redis");

//    properties.setProperty("mysql.version", MYSQL_VER);
//    properties.setProperty("mysql.containerName", "temp_mysql");
//    properties.setProperty("mysql.port", "7306");

    ContainerFactory factory = new ContainerFactory(properties);

    // start all containers
    factory.startContainers();

    // get a container
    Container postgres = factory.container("postgres");

    // for a DB container we can get JDBC URL & Connection
    String jdbcUrl = postgres.config().jdbcUrl();
    assertEquals(jdbcUrl, "jdbc:postgresql://localhost:9823/test_db");
    Connection connection = postgres.config().createConnection();
    connection.close();

    // stop all containers
    factory.stopOnly();
    factory.stopContainers();
  }

}
