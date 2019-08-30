package io.ebean.docker.commands;

import io.ebean.docker.commands.process.ProcessHandler;
import io.ebean.docker.container.Container;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Commands for controlling a cockroachDB docker container.
 */
public class CockroachContainer extends BaseDbContainer implements Container {

  /**
   * Create Postgres container with configuration from properties.
   */
  public static CockroachContainer create(String version, Properties properties) {
    return new CockroachContainer(new CockroachConfig(version, properties));
  }

  /**
   * Create with configuration.
   */
  public CockroachContainer(CockroachConfig config) {
    super(config);
  }

  @Override
  protected boolean isDatabaseAdminReady() {
    return execute("database_name", showDatabases());
  }

  @Override
  protected boolean isFastStartDatabaseExists() {
    return databaseExists();
  }

  @Override
  protected void createDbPreConnectivity() {
    if (!databaseExists()) {
      createDatabase();
    }
  }

  @Override
  protected void dropCreateDbPreConnectivity() {
    dropDatabaseIfExists();
    createDatabase();
  }

  /**
   * Return true if the database exists.
   */
  private boolean databaseExists() {
    final List<String> outLines = ProcessHandler.process(showDatabases()).getOutLines();
    return stdoutContains(outLines, dbConfig.getDbName());
  }

  protected boolean createDatabase() {
    if (execute("CREATE DATABASE", procCreateDb(), "Failed to create database with owner")) {
      //runDbSqlFile(dbName, dbUser, initSqlFile);
      //runDbSqlFile(dbName, dbUser, seedSqlFile);
      return true;
    }
    return false;
  }

  protected boolean dropDatabaseIfExists() {
    ProcessBuilder pb = sqlProcess("drop database if exists " + dbConfig.getDbName());
    return execute("DROP DATABASE", pb, "Failed to drop database");
  }

  /**
   * Wait for the 'database system is ready'
   */
  public boolean isDatabaseReady() {
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return true;
  }

  private ProcessBuilder procCreateDb() {
    return sqlProcess("create database " + dbConfig.getDbName());
  }

  private ProcessBuilder showDatabases() {
    return sqlProcess("show databases");
  }

  private ProcessBuilder sqlProcess(String sql) {
    List<String> args = new ArrayList<>();
    args.add(config.docker);
    args.add("exec");
    args.add("-i");
    args.add(config.containerName());
    args.add("./cockroach");
    args.add("sql");
    args.add("--insecure");
    args.add("-e");
    args.add(sql);
    return createProcessBuilder(args);
  }

  @Override
  protected ProcessBuilder runProcess() {

//    docker run -d \
//    --name=roach1 \
//    --hostname=roach1 \
//    --net=roachnet \
//    -p 26257:26257 -p 8080:8080  \
//    -v "${PWD}/cockroach-data/roach1:/cockroach/cockroach-data"  \
//    cockroachdb/cockroach:v19.1.4 start --insecure
//
    List<String> args = new ArrayList<>();
    args.add(config.docker);
    args.add("run");
    args.add("-d");
    args.add("--name");
    args.add(config.containerName());
    args.add("--hostname=roach1");
//    args.add("--net=roachnet");
//    args.add("--listen-addr=localhost");
    args.add("-p");
    args.add(config.getPort() + ":" + config.getInternalPort());
    args.add("-p");
    args.add(config.getAdminPort() + ":" + config.getAdminInternalPort());

    args.add(config.getImage());
    args.add("start");
    args.add("--insecure");

    return createProcessBuilder(args);
  }

}
