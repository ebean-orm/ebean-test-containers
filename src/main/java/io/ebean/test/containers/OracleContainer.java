package io.ebean.test.containers;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Commands for controlling an Oracle docker container.
 */
public class OracleContainer extends JdbcBaseDbContainer implements Container {

  /**
   * Create a builder for OracleContainer.
   */
  public static Builder builder(String version) {
    return new Builder(version);
  }

  /**
   * Deprecated - migrate to builder().
   */
  @Deprecated
  public static Builder newBuilder(String version) {
    return builder(version);
  }

  public static class Builder extends DbConfig<OracleContainer, OracleContainer.Builder> {

    private String apexPort = "8181";
    private String internalApexPort = "8080";
    /**
     * Wait time allowed when starting oracle from scratch.
     */
    private int startupWaitMinutes = 8;

    private Builder(String version) {
      super("oracle", 1521, 1521, version);
      this.image = "vitorfec/oracle-xe-18c:" + version;
      this.adminUsername = "system";
      this.adminPassword = "oracle";
      this.dbName = "XE";
      this.username = "test_user";
    }

    @Override
    protected String buildJdbcUrl() {
      return "jdbc:oracle:thin:@" + getHost() + ":" + getPort() + ":" + getDbName();
    }

    /**
     * Set the Oracle instance sid. Defaults to XE.
     */
    @Override
    public Builder dbName(String dbName) {
      return super.dbName(dbName);
    }

    /**
     * Set the Oracle user. Defaults to test_user.
     */
    @Override
    public Builder user(String user) {
      return super.user(user);
    }

    /**
     * Set the Apex port.
     */
    public Builder apexPort(String apexPort) {
      this.apexPort = apexPort;
      return self();
    }

    /**
     * Set the internal apex port.
     */
    public Builder internalApexPort(String internalApexPort) {
      this.internalApexPort = internalApexPort;
      return self();
    }

    /**
     * Set the max startup wait time in minutes.
     */
    public Builder startupWaitMinutes(int startupWaitMinutes) {
      this.startupWaitMinutes = startupWaitMinutes;
      return self();
    }

    @Override
    public OracleContainer build() {
      return new OracleContainer(this);
    }
  }

  private final String apexPort;
  private final String internalApexPort;
  private final int startupWaitMinutes;
  private boolean oracleScript;

  /**
   * Create with configuration.
   */
  public OracleContainer(Builder builder) {
    super(builder);
    this.apexPort = builder.apexPort;
    this.internalApexPort = builder.internalApexPort;
    this.startupWaitMinutes = builder.startupWaitMinutes;
    this.checkConnectivityUsingAdmin = true;
    this.waitForConnectivityAttempts = 2000;
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
      createUser(connection, withDrop);

    } catch (SQLException e) {
      throw new RuntimeException("Error when creating database and role", e);
    }
  }

  private void sqlRunOracleScript(Connection connection) {
    if (!oracleScript) {
      sqlRun(connection, "alter session set \"_ORACLE_SCRIPT\"=true");
      oracleScript = true;
    }
  }

  private void dropUser(Connection connection) {
    if (userExists(connection)) {
      sqlRunOracleScript(connection);
      sqlRun(connection, "drop user " + dbConfig.getUsername() + " cascade");
    }
  }

  private void createUser(Connection connection, boolean withDrop) {
    if (withDrop || !userExists(connection)) {
      sqlRunOracleScript(connection);
      sqlRun(connection, "create user " + dbConfig.getUsername() + " identified by " + dbConfig.getPassword());
      sqlRun(connection, "grant connect, resource,  create view, unlimited tablespace to " + dbConfig.getUsername());
    }
  }

  private boolean userExists(Connection connection) {
    String sql = "select 1 from dba_users where lower(username) = '" + dbConfig.getUsername().toLowerCase() + "'";
    return sqlHasRow(connection, sql);
  }

  @Override
  protected ProcessBuilder runProcess() {
    List<String> args = dockerRun();
    args.add("-p");
    args.add(apexPort + ":" + internalApexPort);
    args.add("-e");
    args.add("ORACLE_PWD=" + dbConfig.getAdminPassword());
    args.add(config.getImage());
    return createProcessBuilder(args);
  }

}
