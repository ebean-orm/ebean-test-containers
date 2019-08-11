package io.ebean.docker.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static io.ebean.docker.commands.process.ProcessHandler.process;

public class NuoDBContainer extends BaseDbContainer {

  /**
   * Create NuoDB container with configuration from properties.
   */
  public static NuoDBContainer create(String version, Properties properties) {
    return new NuoDBContainer(new NuoDBConfig(version, properties));
  }

  private final NuoDBConfig nuoConfig;
  private final String network;
  private final String adName;
  private final String smName;
  private final String teName;

  public NuoDBContainer(NuoDBConfig config) {
    super(config);
    this.checkConnectivityUingAdmin = true;
    config.initDefaultSchema();
    this.nuoConfig = config;
    this.network = config.getNetwork();
    this.adName = nuoConfig.containerName();
    this.smName = adName + "_" + nuoConfig.getSm1();
    this.teName = adName + "_" + nuoConfig.getTe1();
  }

  @Override
  public void stopRemove() {
    commands.stopRemove(teName);
    commands.stopRemove(smName);
    commands.stopRemove(adName);
    if (networkExists()) {
      removeNetwork();
    }
  }

  private void removeNetwork() {
    process(procNetworkRemove());
  }

  @Override
  public void stopOnly() {
    commands.stopIfRunning(teName);
    commands.stopIfRunning(smName);
    commands.stopIfRunning(adName);
  }

  @Override
  void runContainer() {
    createNetwork();
    process(runAdminProcess());
    if (waitForAdminProcess()) {
      process(runStorageManager());
      if (waitForStorageManager()) {
        process(runTransactionManager());
      }
    }
  }

  private boolean waitForStorageManager() {
    return waitForLogs(smName, "Node state transition");
  }

  private boolean waitForAdminProcess() {
    return waitForLogs(config.containerName(), "NuoAdmin Server running");
  }

  private boolean waitForLogs(String containerName, String match) {
    for (int i = 0; i < 100; i++) {
      if (logsContain(containerName, match, null)) {
        return true;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return false;
  }

  @Override
  void startContainer() {
    commands.start(adName);
    commands.start(smName);
    commands.start(teName);
  }

  private void createNetwork() {
    if (!networkExists()) {
      process(procNetworkCreate());
    }
  }

  private boolean networkExists() {
    return execute(network, procNetworkList());
  }

  private ProcessBuilder procNetworkCreate() {

    List<String> args = new ArrayList<>();
    args.add(config.docker);
    args.add("network");
    args.add("create");
    args.add(network);
    return createProcessBuilder(args);
  }

  private ProcessBuilder procNetworkRemove() {

    List<String> args = new ArrayList<>();
    args.add(config.docker);
    args.add("network");
    args.add("rm");
    args.add(network);
    return createProcessBuilder(args);
  }

  private ProcessBuilder procNetworkList() {

    List<String> args = new ArrayList<>();
    args.add(config.docker);
    args.add("network");
    args.add("ls");
    args.add("-f");
    args.add("name=" + network);
    return createProcessBuilder(args);
  }

  @Override
  protected ProcessBuilder runProcess() {
    throw new RuntimeException("Not used for NuoDB container");
  }

  private ProcessBuilder runAdminProcess() {

    List<String> args = new ArrayList<>();
    args.add(config.docker);
    args.add("run");
    args.add("-d");
    args.add("--name");
    args.add(adName);
    args.add("--hostname");
    args.add(adName);
    args.add("--net");
    args.add(network);
    args.add("-p");
    args.add(config.getPort() + ":" + config.getInternalPort());
    args.add("-p");
    args.add(nuoConfig.getPort2() + ":" + nuoConfig.getInternalPort2());
    args.add("-p");
    args.add(nuoConfig.getPort3() + ":" + nuoConfig.getInternalPort3());

    if (defined(dbConfig.getAdminPassword())) {
      args.add("-e");
      args.add("NUODB_DOMAIN_ENTRYPOINT=" + adName);
    }
    args.add(config.getImage());
    args.add("nuoadmin");
    return createProcessBuilder(args);
  }

  private ProcessBuilder runStorageManager() {

    // volumes for backup and archive not added yet
    // as generally we are application testing with this

    List<String> args = new ArrayList<>();
    args.add(config.docker);
    args.add("run");
    args.add("-d");
    args.add("--name");
    args.add(smName);
    args.add("--hostname");
    args.add(smName);
    args.add("--net");
    args.add(network);
    args.add(config.getImage());
    args.add("nuodocker");
    args.add("--api-server");
    args.add(adName + ":" + config.getPort());
    args.add("start");
    args.add("sm");
    args.add("--db-name");
    args.add(dbConfig.getDbName());
    args.add("--server-id");
    args.add(adName);
    args.add("--dba-user");
    args.add(dbConfig.getAdminUsername());
    args.add("--dba-password");
    args.add(dbConfig.getAdminPassword());
    args.add("--labels");
    args.add(nuoConfig.getLabels());
    args.add("--archive-dir");
    args.add("/var/opt/nuodb/archive");

    return createProcessBuilder(args);
  }

  private ProcessBuilder runTransactionManager() {

//    docker run -d --name te1 --hostname te1 --rm \
//    --net nuodb-net ${IMG_NAME} nuodocker \
//    --api-server ad1:8888 start te \
//    --db-name test --server-id ad1

    List<String> args = new ArrayList<>();
    args.add(config.docker);
    args.add("run");
    args.add("-d");
    args.add("--name");
    args.add(teName);
    args.add("--hostname");
    args.add(teName);
    args.add("--net");
    args.add(network);
    args.add(config.getImage());
    args.add("nuodocker");
    args.add("--api-server");
    args.add(adName + ":" + config.getPort());
    args.add("start");
    args.add("te");
    args.add("--db-name");
    args.add(dbConfig.getDbName());
    args.add("--server-id");
    args.add(adName);

    return createProcessBuilder(args);
  }

  @Override
  public boolean isDatabaseReady() {
    return commands.logsContain(config.containerName(), "NuoAdmin Server running");
  }

  @Override
  protected boolean isDatabaseAdminReady() {
    return true;
  }

  @Override
  protected void createDbPreConnectivity() {
    // do nothing, we setup via JDBC
  }

  @Override
  protected void dropCreateDbPreConnectivity() {
    // do nothing, we setup via JDBC
  }

  @Override
  protected void createDbPostConnectivity() {
    createSchemaAndUser(false);
  }

  @Override
  protected void dropCreateDbPostConnectivity() {
    createSchemaAndUser(true);
  }

  private void createSchemaAndUser(boolean withDrop) {

    try (Connection connection = config.createAdminConnection()) {

      if (withDrop) {
        sqlDropSchema(connection, dbConfig.getSchema());
      }

      final boolean schemaExists = sqlSchemaExists(connection, dbConfig.getSchema());
      if (!schemaExists) {
        sqlCreateSchema(connection, dbConfig.getSchema());
      }

      final boolean userExists = sqlUserExists(connection, dbConfig.getUsername());
      if (!userExists) {
        sqlCreateUser(connection, dbConfig.getUsername(), dbConfig.getPassword());
      }
      if (withDrop || !userExists) {
        sqlUserGrants(connection, dbConfig.getSchema(), dbConfig.getUsername());
      }
      connection.commit();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private void sqlDropSchema(Connection connection, String schema) throws SQLException {
    exeSql(connection, "drop schema " + schema + " cascade if exists");
  }

  private void sqlUserGrants(Connection connection, String schema, String username) throws SQLException {
    exeSql(connection, "grant create on schema " + schema + " to " + username);
  }

  private void sqlCreateSchema(Connection connection, String schema) throws SQLException {
    exeSql(connection, "create schema " + schema);
  }

  private void sqlCreateUser(Connection connection, String username, String password) throws SQLException {
    exeSql(connection, "create user " + username + " password '" + password + "'");
  }

  private boolean sqlSchemaExists(Connection connection, String schemaName) throws SQLException {
    return sqlQueryMatch(connection, "select schema from system.schemas", schemaName);
  }

  private boolean sqlUserExists(Connection connection, String dbUser) throws SQLException {
    return sqlQueryMatch(connection, "select username from system.users", dbUser);
  }

  private boolean sqlQueryMatch(Connection connection, String sql, String dbUser) throws SQLException {
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      try (ResultSet rset = stmt.executeQuery()) {
        while (rset.next()) {
          final String name = rset.getString(1);
          if (name.equalsIgnoreCase(dbUser)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private void exeSql(Connection connection, String sql) throws SQLException {
    log.debug("exeSql {}", sql);
    try (PreparedStatement st = connection.prepareStatement(sql)) {
      st.execute();
    }
  }
}
