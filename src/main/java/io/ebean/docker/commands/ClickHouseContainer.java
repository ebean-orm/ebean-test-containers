package io.ebean.docker.commands;

import io.ebean.docker.commands.process.ProcessHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ClickHouseContainer extends BaseDbContainer {

  /**
   * Create ClickHouse container with configuration from properties.
   */
  public static ClickHouseContainer create(String version, Properties properties) {
    return new ClickHouseContainer(new ClickHouseConfig(version, properties));
  }

  ClickHouseContainer(ClickHouseConfig config) {
    super(config);
  }

  @Override
  public boolean userExists(String dbUser) {
    // nothing to do
    return true;
  }

  @Override
  protected boolean createUser(String user, String pwd) {
    // nothing to do
    return true;
  }

  @Override
  protected boolean dropUser(String dbUser) {
    // nothing to do
    return true;
  }

  @Override
  protected void createDatabaseExtensionsFor(String dbExtn, String dbName) {
    // do nothing
  }

  @Override
  protected boolean isDatabaseAdminReady() {
    return true;
  }

  @Override
  public boolean databaseExists(String dbName) {
    return execute(dbName, sqlProcess("SHOW DATABASES"));
  }

  @Override
  protected boolean createDatabase(String dbName, String dbUser, String initSqlFile, String seedSqlFile) {
    return hasZeroRows(sqlProcess("CREATE DATABASE " + dbName));
  }

  @Override
  protected void executeSqlFile(String dbUser, String dbName, String containerFilePath) {

  }

  @Override
  protected boolean dropDatabase(String dbName) {
    return hasZeroRows(sqlProcess("DROP DATABASE IF EXISTS " + dbName));
  }

  private boolean hasZeroRows(ProcessBuilder pb) {
    return hasZeroRows(ProcessHandler.process(pb).getOutLines());
  }

  private boolean hasZeroRows(List<String> stdOutLines) {
    return stdOutLines.isEmpty();
  }

  @Override
  protected ProcessBuilder runProcess() {

    //$ docker run -d --name some-clickhouse-server --ulimit nofile=262144:262144 yandex/clickhouse-server

    List<String> args = new ArrayList<>();
    args.add(config.docker);
    args.add("run");
    args.add("-d");
    args.add("--name");
    args.add(config.containerName());
    args.add("--ulimit");
    args.add("nofile=262144:262144");

    args.add("-p");
    args.add(config.getPort() + ":" + config.getInternalPort());
    //8123 port for HTTP interface and 9000 port for native client.

    args.add(config.getImage());
    ProcessBuilder pb = createProcessBuilder(args);
    pb.redirectErrorStream(true);
    return pb;
  }

  @Override
  public boolean isDatabaseReady() {
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return true;
  }

  private ProcessBuilder sqlProcess(String sql) {
    List<String> args = new ArrayList<>();
    args.add(config.docker);
    args.add("exec");
    args.add("-i");
    args.add(config.containerName());
    args.add("clickhouse");
    args.add("client");
    args.add("--query");
    args.add(sql + ";");
    return createProcessBuilder(args);
  }
}
