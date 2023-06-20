package io.ebean.test.containers;

import io.ebean.test.containers.process.ProcessHandler;
import io.ebean.test.containers.process.ProcessResult;

import java.lang.System.Logger.Level;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NuoDBContainer extends JdbcBaseDbContainer {

  @Override
  public NuoDBContainer start() {
    startOrThrow();
    return this;
  }

  /**
   * Create a builder for NuoDB container.
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

  public static class Builder extends DbConfig<NuoDBContainer, NuoDBContainer.Builder> {

    private String network;
    private String sm1 = "sm";
    private String te1 = "te";

    private Builder(String version) { //4.0
      super("nuodb", 48004, 48004, version);
      this.containerName = platform;
      this.image = "nuodb/nuodb-ce:" + version;
      this.adminUsername = "dba";
      this.adminPassword = "dba";
      this.adminPort = 8888;
      this.adminInternalPort = 8888;
      // for testing purposes generally going to use single 'testdb'
      // and different apps have different schema
      this.dbName = "testdb";
    }

    @Override
    protected String buildSummary() {
      return "host:" + host + " port:" + port + " db:" + dbName + " schema:" + schema + " user:" + deriveUsername() + "/" + password;
    }

    @Override
    protected String buildJdbcUrl() {
      return "jdbc:com.nuodb://" + getHost() + ":" + getPort() + "/" + getDbName();
    }

    public Builder network(String network) {
      this.network = network;
      return self();
    }

    public Builder sm1(String sm1) {
      this.sm1 = sm1;
      return self();
    }

    public Builder te1(String te1) {
      this.te1 = te1;
      return self();
    }

    private String getSm1() {
      return sm1;
    }

    private String getTe1() {
      return te1;
    }

    private String getNetwork() {
      return network;
    }

    @Override
    public NuoDBContainer build() {
      return new NuoDBContainer(this);
    }

    @Override
    public NuoDBContainer start() {
      return build().start();
    }
  }

  private static final String AD_RESET = "Entering initializing for server";
  private static final String AD_RUNNING = "NuoAdmin Server running";
  private static final String SM_RESET = "Starting Storage Manager";
  private static final String SM_RUNNING = "Database formed";
  private static final String SM_UNABLE_TO_CONNECT = "Unable to connect ";
  private static final String TE_RESET = "Starting Transaction Engine";
  private static final String TE_RUNNING = "Database entered";

  private final String network;
  private final String adName;
  private final String smName;
  private final String teName;

  private NuoDBContainer(Builder builder) {
    super(builder);
    this.checkConnectivityUsingAdmin = true;
    builder.initDefaultSchema();
    builder.internalConfig().setDefaultContainerName();
    this.adName = config.containerName();
    String network_ = builder.getNetwork();
    this.network = network_ == null ? adName + "-net" : network_;
    this.smName = adName + "_" + builder.getSm1();
    this.teName = adName + "_" + builder.getTe1();
  }

  @Override
  public void stopRemove() {
    if (stopDatabase()) {
      commands.removeContainers(teName, smName, adName);
    }
    if (networkExists()) {
      removeNetwork();
    }
  }

  private void removeNetwork() {
    ProcessHandler.process(procNetworkRemove());
  }

  @Override
  public void stopIfRunning() {
    stopDatabase();
  }

  private boolean stopDatabase() {
    //  nuocmd shutdown database --db-name testdb
    List<String> args = new ArrayList<>();
    args.add(config.docker());
    args.add("exec");
    args.add("-i");
    args.add(adName);
    args.add("nuocmd");
    args.add("shutdown");
    args.add("database");
    args.add("--db-name");
    args.add(dbConfig.getDbName());

    final ProcessResult result = ProcessHandler.process(createProcessBuilder(args));
    if (!result.success()) {
      log.log(Level.ERROR, "Error performing shutdown database " + result);
      return false;
    }
    waitTime(100);
    commands.stop(adName);
    return true;
  }

  @Override
  void runContainer() {
    createNetwork();
    ProcessHandler.process(runAdminProcess());
    if (waitForAdminProcess()) {
      ProcessHandler.process(runStorageManager());
      if (waitForStorageManager()) {
        ProcessHandler.process(runTransactionManager());
        waitForTransactionManager();
      }
    }
  }

  private boolean waitForTransactionManager() {
    return waitForLogs(teName, TE_RUNNING, TE_RESET) && waitTime(100);
  }

  private boolean storageManagerUnableToConnect() {
    boolean unableToConnect = false;

    final List<String> logs = commands.logs(smName);
    for (String log : logs) {
      if (log.contains(SM_UNABLE_TO_CONNECT)) {
        unableToConnect = true;
      } else if (log.contains(SM_RUNNING)) {
        unableToConnect = false;
      }
    }
    return unableToConnect;
  }

  private boolean waitForStorageManager() {
    return waitForLogs(smName, SM_RUNNING, SM_RESET);
  }

  private boolean waitForAdminProcess() {
    return waitForLogs(config.containerName(), AD_RUNNING, AD_RESET);
  }

  private boolean waitTime(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      e.printStackTrace();
    }
    return true;
  }

  private boolean waitForLogs(String containerName, String match, String resetMatch) {
    for (int i = 0; i < 150; i++) {
      if (logsContain(containerName, match, resetMatch)) {
        return true;
      }
      try {
        int sleep = (i < 10) ? 10 : (i < 20) ? 20 : 100;
        Thread.sleep(sleep);
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
    if (!waitForAdminProcess() || !waitForDatabaseState()) {
      throw new RuntimeException("Failed waiting for NuoDB admin container [" + smName + "] to start running");
    } else {
      if (!startStorageManager(0)) {
        throw new RuntimeException("Failed to start storage manager NuoDB [" + adName + "]");
      } else {
        commands.start(teName);
        if (!waitForTransactionManager()) {
          throw new RuntimeException("Failed waiting for NuoDB transaction manager [" + smName + "] to start running");
        }
      }
    }
  }

  private boolean waitForDatabaseState() {
    waitTime(100);
    for (int i = 0; i < 20; i++) {
      if (checkDbStateOk()) {
        return true;
      } else {
        waitTime(100);
      }
    }
    return false;
  }

  private boolean checkDbStateOk() {
    //$ nuocmd show database  --db-format 'dbState:{state}'  --db-name testdb
    List<String> args = new ArrayList<>();
    args.add(config.docker());
    args.add("exec");
    args.add("-i");
    args.add(adName);
    args.add("nuocmd");
    args.add("show");
    args.add("database");
    args.add("--db-format");
    args.add("dbState:{state}");
    args.add("--db-name");
    args.add(dbConfig.getDbName());
    try {
      final ProcessResult result = ProcessHandler.process(createProcessBuilder(args));
      if (result.success()) {
        for (String outLine : result.getOutLines()) {
          final String trimmedOut = outLine.trim();
          if (trimmedOut.startsWith("dbState:")) {
            return dbStateOk(trimmedOut);
          }
        }
      }
    } catch (CommandException e) {
      return false;
    }
    return false;
  }

  private boolean dbStateOk(String trimmedOut) {
    log.log(Level.TRACE, "checking dbStateOk [{0}]", trimmedOut);
    return trimmedOut.contains("NOT_RUNNING") || trimmedOut.contains("RUNNING");
  }

  private boolean startStorageManager(int attempt) {
    commands.start(smName);
    if (!waitForStorageManager()) {
      log.log(Level.ERROR, "Failed waiting for NuoDB storage manager [" + adName + "] to start running");
      return false;
    }
    if (storageManagerUnableToConnect()) {
      log.log(Level.INFO, "Retry NuoDB storage manager [" + adName + "] attempt:" + attempt);
      return attempt <= 2 && startStorageManager(attempt + 1);
    }
    return true;
  }

  private void createNetwork() {
    if (!networkExists()) {
      ProcessHandler.process(procNetworkCreate());
    }
  }

  private boolean networkExists() {
    return execute(network, procNetworkList());
  }

  private ProcessBuilder procNetworkCreate() {
    List<String> args = new ArrayList<>();
    args.add(config.docker());
    args.add("network");
    args.add("create");
    args.add(network);
    return createProcessBuilder(args);
  }

  private ProcessBuilder procNetworkRemove() {
    List<String> args = new ArrayList<>();
    args.add(config.docker());
    args.add("network");
    args.add("rm");
    args.add(network);
    return createProcessBuilder(args);
  }

  private ProcessBuilder procNetworkList() {
    List<String> args = new ArrayList<>();
    args.add(config.docker());
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
    args.add(config.docker());
    args.add("run");
    args.add("-d");
    args.add("--name");
    args.add(adName);
    args.add("--hostname");
    args.add(adName);
    args.add("--network");
    args.add(network);
    args.add("-p");
    args.add(config.getPort() + ":" + config.getInternalPort());
    args.add("--env");
    args.add("NUODB_DOMAIN_ENTRYPOINT=" + adName);
    args.add(config.getImage());
    args.add("nuoadmin");
    return createProcessBuilder(args);
  }

  private ProcessBuilder runStorageManager() {
    List<String> args = new ArrayList<>();
    args.add(config.docker());
    args.add("run");
    args.add("-d");
    args.add("--name");
    args.add(smName);
    args.add("--hostname");
    args.add(smName);
    args.add("--network");
    args.add(network);
    args.add(config.getImage());
    args.add("nuodocker");
    args.add("--api-server");
    args.add(adName + ":" + config.getAdminInternalPort());
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
    return createProcessBuilder(args);
  }

  private ProcessBuilder runTransactionManager() {
    List<String> args = new ArrayList<>();
    args.add(config.docker());
    args.add("run");
    args.add("-d");
    args.add("--name");
    args.add(teName);
    args.add("--hostname");
    args.add(teName);
    args.add("--network");
    args.add(network);
    args.add(config.getImage());
    args.add("nuodocker");
    args.add("--api-server");
    args.add(adName + ":" + config.getAdminInternalPort());
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
  void createDatabase() {
    createSchemaAndUser(false);
  }

  @Override
  void dropCreateDatabase() {
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

  private void exeSql(Connection connection, String sql) throws SQLException {
    log.log(Level.DEBUG, "exeSql {0}", sql);
    try (PreparedStatement st = connection.prepareStatement(sql)) {
      st.execute();
    }
  }
}
