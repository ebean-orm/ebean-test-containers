package org.avaje.docker.container;

import org.junit.Test;

import java.sql.Connection;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class ContainerFactoryTest {

  @Test
  public void create() throws Exception {

    Properties properties = new Properties();
    properties.setProperty("postgres.version", "9.6");
    properties.setProperty("postgres.containerName", "junk_postgres");
    properties.setProperty("postgres.port", "9823");

    properties.setProperty("elastic.version", "5.6.0");
    properties.setProperty("elastic.port", "9201");

//    properties.setProperty("mysql.version", "5.7");
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
