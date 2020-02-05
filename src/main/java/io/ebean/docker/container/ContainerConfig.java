package io.ebean.docker.container;

import io.ebean.docker.commands.StartMode;
import io.ebean.docker.commands.StopMode;

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
   * Return the image version.
   */
  String version();

  /**
   * Return a DB connection url or null if not a database container.
   */
  String jdbcUrl();

  /**
   * Return a DB connection url for the admin database user.
   */
  String jdbcAdminUrl();

  /**
   * Return a DB connection.
   */
  Connection createConnection() throws SQLException;

  /**
   * Return a DB connection without schema (as it maybe is not created yet).
   */
  Connection createConnectionNoSchema() throws SQLException;

  /**
   * Return a DB connection using the admin user.
   */
  Connection createAdminConnection() throws SQLException;

  /**
   * Set the start mode.  One of create, dropCreate, or container [only].
   */
  void setStartMode(StartMode startMode);

  /**
   * Set the stop mode used when stop() is called.
   */
  void setStopMode(StopMode stopMode);

  /**
   * Set the shutdown hook mode to automatically stop/remove the container on JVM shutdown.
   */
  void setShutdownMode(StopMode shutdownHookMode);

  /**
   * Return a good description for starting the container typically for logging.
   */
  String startDescription();

  /**
   * Return a good description for stopping the container typically for logging.
   */
  String stopDescription();
}
