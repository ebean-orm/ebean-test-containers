package org.avaje.docker.commands;

import org.avaje.docker.container.Container;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MySqlContainer extends DbContainer implements Container {

  public static MySqlContainer create(String mysqlVersion, Properties properties) {
    return new MySqlContainer(new MySqlConfig(mysqlVersion, properties));
  }

  public MySqlContainer(MySqlConfig config) {
    super(config);
  }

  @Override
  protected boolean isDatabaseReady() {
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

    if (defined(dbConfig.getDbAdminPassword())) {
      args.add("-e");
      args.add("MYSQL_ROOT_PASSWORD=" + dbConfig.getDbAdminPassword());
    }
    if (defined(dbConfig.getDbName())) {
      args.add("-e");
      args.add("MYSQL_DATABASE=" + dbConfig.getDbName());
    }
    if (defined(dbConfig.getDbUser())) {
      args.add("-e");
      args.add("MYSQL_USER=" + dbConfig.getDbUser());
    }
    if (defined(dbConfig.getDbPassword())) {
      args.add("-e");
      args.add("MYSQL_PASSWORD=" + dbConfig.getDbPassword());
    }

    args.add(config.getImage());

    return createProcessBuilder(args);
  }

}
