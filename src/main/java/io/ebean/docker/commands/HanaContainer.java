package io.ebean.docker.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ebean.docker.container.Container;

/**
 * Commands for controlling a SAP HANA docker container.
 */
public class HanaContainer extends DbContainer implements Container {

  /**
   * Create SAP HANA container with configuration from properties.
   */
  public static HanaContainer create(String version, Properties properties) {
    return new HanaContainer(new HanaConfig(version, properties));
  }

  private static final Logger log = LoggerFactory.getLogger(Commands.class);

  private final HanaConfig hanaConfig;

  /**
   * Create with configuration.
   */
  public HanaContainer(HanaConfig config) {
    super(config);
    this.hanaConfig = config;
    String osName = System.getProperty("os.name").toLowerCase();
    if (!osName.contains("linux")) {
      throw new IllegalStateException("The HANA docker image requires a Linux operating system");
    }
    if (!hanaConfig.isAgreeToSapLicense()) {
      throw new IllegalStateException(
          "You must agree to the SAP license (https://www.sap.com/docs/download/cmp/2016/06/sap-hana-express-dev-agmt-and-exhibit.pdf) by setting the property 'hana.agreeToSapLicense' to 'true'");
    }
  }

  @Override
  protected boolean isDatabaseAdminReady() {
    return isDatabaseReady();
  }

  @Override
  protected boolean isDatabaseReady() {
    return commands.logsContain(config.containerName(), "Startup finished!");
  }

  /**
   * Start the container and wait for it to be ready.
   * <p>
   * This checks if the container is already running.
   * </p>
   * <p>
   * Returns false if the wait for ready was unsuccessful.
   * </p>
   */
  @Override
  public boolean startWithCreate() {
    startMode = Mode.Create;
    startIfNeeded();
    if (!waitForDatabaseReady()) {
      log.warn("Failed waitForDatabaseReady for container {}", config.containerName());
      return false;
    }
    if (!createUserIfNotExists()) {
      return false;
    }
    if (!waitForConnectivity()) {
      log.warn("Failed waiting for connectivity");
      return false;
    }
    return true;
  }

  /**
   * Start with a drop and create of the database and user.
   */
  @Override
  public boolean startWithDropCreate() {
    startMode = Mode.DropCreate;
    startIfNeeded();
    if (!waitForDatabaseReady()) {
      log.warn("Failed waitForDatabaseReady for container {}", config.containerName());
      return false;
    }

    dropUserIfExists();

    if (!createUserIfNotExists()) {
      return false;
    }
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
    args.add("-p");
    args.add("3" + hanaConfig.getInstanceNumber() + "13:39013");
    args.add("-p");
    args.add(config.getPort() + ":" + config.getInternalPort());
    args.add("-p");
    args.add("3" + hanaConfig.getInstanceNumber() + "41-3" + hanaConfig.getInstanceNumber() + "45:39041-39045");
    args.add("-v");
    args.add(hanaConfig.getMountsDirectory() + ":/hana/mounts");
    args.add("--ulimit");
    args.add("nofile=1048576:1048576");
    args.add("--sysctl");
    args.add("kernel.shmmax=1073741824");
    args.add("--sysctl");
    args.add("kernel.shmmni=524288");
    args.add("--sysctl");
    args.add("kernel.shmall=8388608");
    args.add("--name");
    args.add(config.containerName());
    args.add(config.getImage());
    args.add("--passwords-url");
    args.add(hanaConfig.getPasswordsUrl().toString());
    if (hanaConfig.isAgreeToSapLicense()) {
      args.add("--agree-to-sap-license");
    }

    return createProcessBuilder(args);
  }

  private boolean dropUserIfExists() {
    log.info("Drop database user {} if exists", dbConfig.getUsername());
    sqlProcess(connection -> {
      if (userExists(connection)) {
        runSql(connection, "drop user " + dbConfig.getUsername() + " cascade");
      }
    });
    return true;
  }

  private boolean createUserIfNotExists() {
    log.info("Create database user {} if not exists", dbConfig.getUsername());
    sqlProcess(connection -> {
      if (!userExists(connection)) {
        runSql(connection, "create user " + dbConfig.getUsername() + " password " + dbConfig.getPassword()
            + " no force_first_password_change");
      }
    });
    return true;
  }

  private boolean userExists(Connection connection) {
    try (PreparedStatement statement = connection
        .prepareStatement("select count(*) from sys.users where user_name = upper(?)")) {
      statement.setString(1, dbConfig.getUsername());
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          int count = rs.getInt(1);
          return count == 1;
        }
        return false;
      }

    } catch (SQLException e) {
      log.error("Failed to execute sql to check if user exists", e);
      return false;
    }
  }

  private boolean sqlProcess(Consumer<Connection> runner) {
    try (Connection connection = config.createAdminConnection()) {
      runner.accept(connection);
      return true;
    } catch (SQLException e) {
      log.error("Failed to execute sql", e);
      return false;
    }
  }

  private void runSql(Connection connection, String sql) {
    try (Statement statement = connection.createStatement()) {
      log.debug("execute: " + sql);
      statement.execute(sql);
    } catch (SQLException e) {
      sneakyThrow(e);
    }
  }

  @SuppressWarnings("unchecked")
  private <E extends Throwable> void sneakyThrow(Throwable t) throws E {
    throw (E) t;
  }
}
