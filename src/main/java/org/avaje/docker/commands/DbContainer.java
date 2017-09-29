package org.avaje.docker.commands;

import org.avaje.docker.container.Container;

import java.sql.Connection;
import java.sql.SQLException;

abstract class DbContainer extends BaseContainer implements Container {

  final DbConfig dbConfig;

  DbContainer(DbConfig config) {
    super(config);
    this.dbConfig = config;
  }

  /**
   * Return the ProcessBuilder used to execute the container run command.
   */
  protected abstract ProcessBuilder runProcess();

  boolean checkConnectivity() {
    try {
      log.debug("checkConnectivity ... ");
      Connection connection = config.createConnection();
      connection.close();
      log.debug("connectivity confirmed ");
      return true;

    } catch (SQLException e) {
      log.trace("connection failed: " + e.getMessage());
      return false;
    }
  }

  boolean userDefined() {
    return defined(dbConfig.getDbUser());
  }

  boolean databaseDefined() {
    return defined(dbConfig.getDbName());
  }

  boolean defined(String val) {
    return val != null && !val.trim().isEmpty();
  }

}
