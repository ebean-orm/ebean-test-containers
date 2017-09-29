package org.avaje.docker.commands;

import org.avaje.docker.container.Container;

import java.util.ArrayList;
import java.util.List;

public class MySqlContainer extends DbContainer implements Container {

  public MySqlContainer(MySqlConfig config) {
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

  @Override
  protected ProcessBuilder runProcess() {

    List<String> args = new ArrayList<>();
    args.add(config.docker);
    args.add("run");
    args.add("-d");
    args.add("--name");
    args.add(config.containerName());
    args.add("-p");
    args.add(config.getPort() + ":" + config.getInternalPort());

    if (defined(config.getDbAdminPassword())) {
      args.add("-e");
      args.add("MYSQL_ROOT_PASSWORD="+config.getDbAdminPassword());
    }
    if (defined(config.getDbName())) {
      args.add("-e");
      args.add("MYSQL_DATABASE="+config.getDbName());
    }
    if (defined(config.getDbUser())) {
      args.add("-e");
      args.add("MYSQL_USER="+config.getDbUser());
    }
    if (defined(config.getDbPassword())) {
      args.add("-e");
      args.add("MYSQL_PASSWORD="+config.getDbPassword());
    }

    args.add(config.getImage());

    return createProcessBuilder(args);
  }

}
