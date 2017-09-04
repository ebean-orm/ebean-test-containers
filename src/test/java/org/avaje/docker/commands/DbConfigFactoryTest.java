package org.avaje.docker.commands;

import org.junit.Test;

import java.util.Properties;

public class DbConfigFactoryTest {

  @Test
  public void create() throws Exception {

    Properties properties = new Properties();
    properties.setProperty("dbPlatform", "postgres");
    properties.setProperty("dbName", "test_roberto");
    properties.setProperty("dbUser", "test_robino");
    properties.setProperty("dbExtensions", "hstore,pgcrypto");

    DbConfig dbConfig = DbConfigFactory.create(properties);

    if (dbConfig.hasPlatform()) {
      DbCommands commands = DbConfigFactory.createCommands(dbConfig);

      commands.start();
      commands.stop();
    }

  }

}
