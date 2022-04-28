package io.ebean.docker.commands;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ClickHouseContainer extends JdbcBaseDbContainer {

  public static class Builder extends DbConfig<ClickHouseContainer, ClickHouseContainer.Builder> {

    private Builder(String version) {
      super("clickhouse", 8123, 8123, version);
      this.image = "yandex/clickhouse-server:" + version;
      this.user("default");
      this.password("");
      this.adminUsername = "default";
      this.adminPassword = "";
    }

    @Override
    protected String buildJdbcUrl() {
      return "jdbc:clickhouse://" + getHost() + ":" + getPort() + "/" + getDbName();
    }

    @Override
    protected String buildJdbcAdminUrl() {
      return "jdbc:clickhouse://" + getHost() + ":" + getPort() + "/default";
    }

    @Override
    public ClickHouseContainer build() {
      return new ClickHouseContainer(this);
    }
  }

  public static Builder newBuilder(String version) {
    return new Builder(version);
  }

  ClickHouseContainer(Builder builder) {
    super(builder);
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
        dropDatabase(connection);
      }
      createDatabase(connection);

    } catch (SQLException e) {
      throw new RuntimeException("Error when creating database and role", e);
    }
  }

  private void dropDatabase(Connection connection) {
    sqlRun(connection, "drop database if exists " + dbConfig.getDbName());
  }

  private void createDatabase(Connection connection) {
    sqlRun(connection, "create database if not exists " + dbConfig.getDbName());
  }

  @Override
  protected ProcessBuilder runProcess() {

    //$ docker run -d --name some-clickhouse-server --ulimit nofile=262144:262144 yandex/clickhouse-server

    List<String> args = new ArrayList<>();
    args.add(config.docker());
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

}
