package io.ebean.docker.commands;

import io.ebean.docker.commands.process.ProcessHandler;
import io.ebean.docker.commands.process.ProcessResult;
import io.ebean.docker.container.Container;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Commands for controlling a DB2 docker container.
 */
public class Db2Container extends JdbcBaseDbContainer implements Container {

  public static Db2Container create(String Db2Version, Properties properties) {
    return new Db2Container(new Db2Config(Db2Version, properties));
  }

  public Db2Container(Db2Config config) {
    super(config);
  }

  @Override
  void createDatabase() {
    if (checkConnectivity(false)) {
      return; // container with DB & userName already running - do not recreate DB
    }
    // #1 Do user management (as root)
    try {
      dockerSu("root", "useradd -g db2iadm1 " + dbConfig.getUsername());
    } catch (CommandException e) {
      e.getResult().getOutLines().forEach(log::warn);
    }
    dockerSu("root", "echo \"" + dbConfig.getUsername() + ":" + dbConfig.getPassword() + "\" | chpasswd");

    // #2 create database (with optional create options)
    String cmd = "db2 create database " + dbConfig.getDbName();
    if (defined(((Db2Config) dbConfig).getCreateOptions())) {
      cmd = cmd + " " + ((Db2Config) dbConfig).getCreateOptions();
    }
    dockerSu(cmd);

    // #3 set optional config options
    if (defined(((Db2Config) dbConfig).getConfigOptions())) {
      cmd = "db2 update database config for " + dbConfig.getDbName() + " " + ((Db2Config) dbConfig).getConfigOptions();
      dockerSu(dbConfig.getAdminUsername(), cmd);
    }

    // #4 activate database
    cmd = "db2 activate database " + dbConfig.getDbName();
    dockerSu(cmd);

    // #5 set recommended values for database
    cmd = "/var/db2_setup/lib/set_rec_values.sh " + dbConfig.getDbName();
    dockerSu(cmd);
  }

  @Override
  void dropCreateDatabase() {
    try {
      dockerSu("db2 drop database " + dbConfig.getDbName());
    } catch (CommandException e) {
      // may fail, if database does not exist
      e.getResult().getOutLines().forEach(log::warn);
    }
    createDatabase();
  }

  @Override
  protected ProcessBuilder runProcess() {

    List<String> args = dockerRun();
    args.add("--privileged"); // this container needs extended permissions
    args.add("-e");
    args.add("LICENSE=accept");

    args.add("-e");
    args.add("SAMPLEDB=false");

    args.add("-e");
    args.add("ARCHIVE_LOGS=false");
    // (default: true) Specify false to not configure log archiving (reduces start
    // up time)

    args.add("-e");
    args.add("AUTOCONFIG=false");
    // (default: true) Specify false to not run auto configuration on the instance
    // and database (reduces start up time)

    // set admin user & pw - but do not create a database
    args.add("-e");
    args.add("DB2INSTANCE=" + dbConfig.getAdminUsername());

    if (defined(dbConfig.getAdminPassword())) {
      args.add("-e");
      args.add("DB2INST1_PASSWORD=" + dbConfig.getAdminPassword());
    }
    args.add("-e");
    args.add("DBNAME=");
    args.add(config.getImage());
    return createProcessBuilder(args);
  }

  /**
   * Runs the given (linux) command inside the container as given user.
   */
  protected List<String> dockerSu(String user, String cmd) {
    List<String> args = new ArrayList<>();
    args.add(config.docker);
    args.add("exec");
    args.add("-i");
    args.add(config.containerName());
    args.add("su");
    args.add("-");
    args.add(user);
    args.add("-c");
    args.add(cmd);
    ProcessBuilder pb = createProcessBuilder(args);
    ProcessResult pr = ProcessHandler.process(pb);
    pr.getOutLines().forEach(log::debug);
    return pr.getOutLines();
  }

  /**
   * Executes the (linux) command as DB-admin user.
   */
  protected List<String> dockerSu(String cmd) {
    return dockerSu(dbConfig.getAdminUsername(), cmd);
  }

  /**
   * used to detect, when the container is up. This does not mean, you can already
   * connect with JDBC. You must create the database first.
   */
  @Override
  boolean checkConnectivity() {
    try {
      List<String> result = dockerSu("db2pd -");

      for (String outLine : result) {
        if (outLine.contains("-- Active --")) {
          return true;
        }
      }
      return false;
    } catch (Throwable t) {
      return false;
    }
  }
}
