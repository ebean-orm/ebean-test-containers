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
  protected boolean isDatabaseAdminReady() {
    return true;
  }

  @Override
  protected void createDbPreConnectivity() {
    if (!databaseExists()) {
      createDatabase();
    }
  }

  @Override
  protected void dropCreateDbPreConnectivity() {
    if (dropDatabase()) {
      createDatabase();
    }
  }

  private boolean databaseExists() {
    return execute(dbConfig.getDbName(), sqlProcess("SHOW DATABASES"));
  }

  private boolean createDatabase() {
    return hasZeroRows(sqlProcess("CREATE DATABASE " + dbConfig.getDbName()));
  }

  private boolean dropDatabase() {
    return hasZeroRows(sqlProcess("DROP DATABASE IF EXISTS " + dbConfig.getDbName()));
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
