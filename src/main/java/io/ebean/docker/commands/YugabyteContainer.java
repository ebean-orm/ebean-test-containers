package io.ebean.docker.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Creates Yugabyte container (with common commands for roles and database as Postgres).
 */
public class YugabyteContainer extends BasePostgresContainer {

  /**
   * Create Yugabyte container with configuration from properties.
   */
  public static YugabyteContainer create(String version, Properties properties) {
    return new YugabyteContainer(new YugabyteConfig(version, properties));
  }

  public YugabyteContainer(YugabyteConfig config) {
    super(config);
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
