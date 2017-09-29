package org.avaje.docker.container;

import org.avaje.docker.container.ContainerFactory;
import org.avaje.docker.container.Container;
import org.junit.Test;

import java.util.Properties;

public class ContainerFactoryTest {

  @Test
  public void create() throws Exception {

    Properties properties = new Properties();
    properties.setProperty("postgres.version", "9.6");
    properties.setProperty("postgres.containerName", "junk_postgres");
    properties.setProperty("postgres.port", "9823");

    properties.setProperty("mysql.version", "5.7");
    properties.setProperty("mysql.containerName", "temp_mysql");
    properties.setProperty("mysql.port", "7306");


    ContainerFactory factory = new ContainerFactory(properties);
    factory.startContainers();
    factory.stopContainers();
  }

}
