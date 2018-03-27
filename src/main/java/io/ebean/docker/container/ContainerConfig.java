package io.ebean.docker.container;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Configuration details associated with a container.
 */
public interface ContainerConfig {

  /**
   * Return the type of container. postgres, mysql, elastic etc.
   */
  String platform();

  /**
   * Return the container name.
   */
  String containerName();

  /**
   * Return a DB connection url or null if not a database container.
   */
  String jdbcUrl();

  /**
   * Return a DB connection.
   */
  Connection createConnection() throws SQLException;

  /**
   * Return a DB connection using the admin user.
   */
  Connection createAdminConnection() throws SQLException;

  /**
   * Set the start mode.  One of create, dropCreate, or container [only].
   */
  void setStartMode(String startMode);

  /**
   * Set the stop mode.
   */
  void setStopMode(String stopMode);

  /**
   * Return a good description for starting the container typically for logging.
   */
  String startDescription();

  /**
   * Return a good description for stopping the container typically for logging.
   */
  String stopDescription();
}
