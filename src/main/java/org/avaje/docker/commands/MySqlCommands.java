package org.avaje.docker.commands;

import java.util.ArrayList;
import java.util.List;

public class MySqlCommands extends BaseDbCommands implements DbCommands {

  public MySqlCommands(DbConfig config) {
    super(config);
  }

  @Override
  public boolean start() {
    startIfNeeded();
    if (!waitForConnectivity()) {
      log.warn("Failed waiting for connectivity");
      return false;
    }
    return true;
  }

  protected String jdbcUrl() {
    return "jdbc:mysql://localhost:" + config.dbPort + "/" + config.dbName;
  }

  @Override
  protected ProcessBuilder runProcess() {

    List<String> args = new ArrayList<>();
    args.add(config.docker);
    args.add("run");
    args.add("-d");
    args.add("--name");
    args.add(config.name);
    args.add("-p");
    args.add(config.dbPort + ":" + config.internalPort);

    if (defined(config.dbAdminPassword)) {
      args.add("-e");
      args.add("MYSQL_ROOT_PASSWORD="+config.dbAdminPassword);
    }
    if (defined(config.dbName)) {
      args.add("-e");
      args.add("MYSQL_DATABASE="+config.dbName);
    }
    if (defined(config.dbUser)) {
      args.add("-e");
      args.add("MYSQL_USER="+config.dbUser);
    }
    if (defined(config.dbPassword)) {
      args.add("-e");
      args.add("MYSQL_PASSWORD="+config.dbPassword);
    }

    args.add(config.image);

    return createProcessBuilder(args);
  }

}
