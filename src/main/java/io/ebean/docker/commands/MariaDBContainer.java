package io.ebean.docker.commands;

import java.util.Properties;

/**
 * MariaDB container. Actually just the same as MySql.
 */
public class MariaDBContainer extends MySqlContainer {

  public static MariaDBContainer create(String version, Properties properties) {
    return new MariaDBContainer(new MariaDBConfig(version, properties));
  }

  public MariaDBContainer(MariaDBConfig config) {
    super(config);
  }

}
