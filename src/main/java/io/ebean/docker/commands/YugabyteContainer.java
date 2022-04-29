package io.ebean.docker.commands;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates Yugabyte container (with common commands for roles and database as Postgres).
 */
public class YugabyteContainer extends BasePostgresContainer {

  /**
   * Create a builder.
   */
  public static Builder newBuilder(String version) {
    return new Builder(version);
  }

  public static class Builder extends DbConfig<YugabyteContainer, YugabyteContainer.Builder> {

    private Builder(String version) {
      super("yugabyte", 6433, 5433, version);
      this.image = "yugabytedb/yugabyte:" + version;
      this.adminUsername = "postgres";
      //setTmpfs("/var/lib/postgresql/data:rw");
    }

    @Override
    protected String buildJdbcUrl() {
      return "jdbc:postgresql://" + getHost() + ":" + getPort() + "/" + getDbName();
    }

    @Override
    protected String buildJdbcAdminUrl() {
      return "jdbc:postgresql://" + getHost() + ":" + getPort() + "/postgres";
    }

    @Override
    public YugabyteContainer build() {
      return new YugabyteContainer(this);
    }
  }

  private YugabyteContainer(Builder builder) {
    super(builder);
  }

  @Override
  protected ProcessBuilder runProcess() {
    List<String> args = new ArrayList<>();
    args.add(config.docker());
    args.add("run");
    args.add("-d");
    args.add("--name");
    args.add(config.containerName());
    args.add("-p");
    args.add(config.getPort() + ":" + config.getInternalPort());
    args.add("-p");
    args.add("7000:7000");
    args.add("-p");
    args.add("9000:9000");
    args.add("-p");
    args.add("9042:9042");
//    if (dbConfig.isInMemory() && dbConfig.getTmpfs() != null) {
//      args.add("--tmpfs");
//      args.add(dbConfig.getTmpfs());
//    }
//    if (!dbConfig.adminPassword.isEmpty()) {
//      args.add("-e");
//      args.add("POSTGRES_PASSWORD=" + dbConfig.getAdminPassword());
//    }
    args.add(config.getImage());
    args.add("bin/yugabyted");
    args.add("start");
    // args.add("--base_dir=/home/yugabyte/yb_data");
    args.add("--daemon=false");
    return createProcessBuilder(args);
  }
}
