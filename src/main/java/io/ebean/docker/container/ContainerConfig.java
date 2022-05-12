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
   * Return the image version.
   */
  String version();

  /**
   * Return the port this container is using.
   * <p>
   * This is typically useful if the container was started with a random port
   * and, we need to know what that port was.
   */
  int port();

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
   * Return a DB connection using the admin user with the given jdbc url.
   */
  Connection createAdminConnection(String url) throws SQLException;

  /**
   * Return a good description for starting the container typically for logging.
   */
  String startDescription();

  /**
   * Return a good description for stopping the container typically for logging.
   */
  String stopDescription();
}
