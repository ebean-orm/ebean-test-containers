package io.ebean.docker.commands;

import io.ebean.docker.container.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

/**
 * Commands for controlling an Oracle docker container.
 */
public class OracleContainer extends JdbcBaseDbContainer implements Container {

  /**
   * Create Postgres container with configuration from properties.
   */
  public static OracleContainer create(String version, Properties properties) {
    return new OracleContainer(new OracleConfig(version, properties));
  }

  private static final Logger log = LoggerFactory.getLogger(Commands.class);

  private final OracleConfig oracleConfig;

  /**
   * Create with configuration.
   */
  public OracleContainer(OracleConfig config) {
    super(config);
    this.oracleConfig = config;
    this.checkConnectivityUsingAdmin = true;
  }

  @Override
  void createDatabase() {
    createRoleAndDatabase(false);
  }

  @Override
  void dropCreateDatabase() {
    createRoleAndDatabase(true);
  }

  private void createRoleAndDatabase(boolean withDrop) {
    try (Connection connection = config.createAdminConnection()) {
      if (withDrop) {
        dropUser(connection);
      }
      createUser(connection);

    } catch (SQLException e) {
      throw new RuntimeException("Error when creating database and role", e);
    }
  }

  private void dropUser(Connection connection) {
    if (userExists(connection)) {
      sqlRun(connection, "drop user " + dbConfig.getUsername() + " cascade");
    }
  }

  private void createUser(Connection connection) {
    sqlRun(connection, "alter session set \"_ORACLE_SCRIPT\"=true");
    sqlRun(connection, "create user " + dbConfig.getUsername() + " identified by " + dbConfig.getPassword());
    sqlRun(connection, "grant connect, resource,  create view, unlimited tablespace to " + dbConfig.getUsername());
  }

  private boolean userExists(Connection connection) {
    String sql = "select 1 from dba_users where lower(username) = '"+dbConfig.getUsername().toLowerCase()+"'";
    return sqlHasRow(connection, sql);
  }

  @Override
  protected ProcessBuilder runProcess() {
    List<String> args = dockerRun();
    args.add("-p");
    args.add(oracleConfig.getApexPort() + ":" + oracleConfig.getInternalApexPort());
    args.add("-e");
    args.add("ORACLE_PWD=" + oracleConfig.getAdminPassword());
    args.add(config.getImage());
    return createProcessBuilder(args);
  }

}
