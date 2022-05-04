package io.ebean.docker.commands;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ebean.docker.container.Container;

/**
 * Commands for controlling a SAP HANA docker container.
 */
public class HanaContainer extends DbContainer implements Container {

  /**
   * Return a builder for HanaContainer.
   */
  public static Builder newBuilder(String version) {
    return new Builder(version);
  }

  /**
   * Check if the user has agreed to the <a href=
   * "https://www.sap.com/docs/download/cmp/2016/06/sap-hana-express-dev-agmt-and-exhibit.pdf">SAP
   * license</a>
   *
   * @return {@code true} if the user has agreed to the license, {@code false}
   * otherwise
   */
  public static boolean checkLicenseAgreement() {
    String propertyValue = System.getProperty("hana.agreeToSapLicense");
    if (propertyValue != null) {
      return Boolean.parseBoolean(propertyValue);
    }
    propertyValue = System.getenv("hana.agreeToSapLicense");
    if (propertyValue != null) {
      return Boolean.parseBoolean(propertyValue);
    }
    return false;
  }

  /**
   * SAP HANA configuration.
   * <p>
   * For more information about the HANA docker configuration see the tutorial
   * <a href="https://developers.sap.com/tutorials/hxe-ua-install-using-docker.html">Installing SAP HANA, express edition with Docker</a>
   */
  public static class Builder extends DbConfig<HanaContainer, Builder> {

    private static final Logger log = LoggerFactory.getLogger(Builder.class);

    private String mountsDirectory;
    private URL passwordsUrl;
    private String instanceNumber;
    private boolean agreeToSapLicense;

    private Builder(String version) {
      super("hana", 39017, 39017, version);
      this.image = "store/saplabs/hanaexpress:" + version;
      this.mountsDirectory = "/data/dockermounts";
      try {
        this.passwordsUrl = new URL("file:///hana/mounts/passwords.json");
      } catch (MalformedURLException e1) {
        log.debug("Invalid passwords URL. Can't happen.");
      }
      this.instanceNumber = "90";
      this.agreeToSapLicense = checkLicenseAgreement();
      this.adminUsername = "SYSTEM";
      this.adminPassword = "HXEHana1";
      this.password = "HXEHana1";
      this.dbName = "HXE";
      this.username = "test_user";
      this.maxReadyAttempts = 3000;
    }

    @Override
    protected void extraProperties(Properties properties) {
      super.extraProperties(properties);
      if (!Integer.toString(this.port).matches("\\d{5}")) {
        throw new IllegalArgumentException("Invalid port: " + this.port + ". The port must consist of exactly 5 digits.");
      }
      this.mountsDirectory = prop(properties, "mountsDirectory", "/data/dockermounts");
      if (!Files.isDirectory(Paths.get(this.mountsDirectory))) {
        throw new IllegalArgumentException(
          "The given mounts directory \"" + this.mountsDirectory + "\" doesn't exist or is not a directory");
      }
      try {
        this.passwordsUrl = new URL(prop(properties, "passwordsUrl", "file:///hana/mounts/passwords.json"));
      } catch (MalformedURLException e) {
        log.warn("Invalid passwords URL. Using default.", e);
        try {
          this.passwordsUrl = new URL("file:///hana/mounts/passwords.json");
        } catch (MalformedURLException e1) {
          log.debug("Invalid passwords URL. Can't happen.");
        }
      }
      this.instanceNumber = prop(properties, "instanceNumber", "90");
      if (!this.instanceNumber.matches("\\d{2}")) {
        throw new IllegalArgumentException("Invalid instance number: " + this.instanceNumber
          + ". The instance number must consist of exactly two digits.");
      }
      if (!"90".equals(this.instanceNumber)) {
        String portStr = Integer.toString(this.port);
        this.port = Integer.parseInt(portStr.substring(0, 1) + this.instanceNumber + portStr.substring(3));
      }
      this.agreeToSapLicense = checkLicenseAgreementFor(properties);
    }

    /**
     * Return the JDBC URL for connecting to the database
     */
    @Override
    protected String buildJdbcUrl() {
      return "jdbc:sap://" + getHost() + ":" + getPort() + "/?databaseName=" + getDbName();
    }

    @Override
    public HanaContainer build() {
      return new HanaContainer(this);
    }

    /**
     * Return the path to the container-external mounts directory that can be used
     * by the HANA docker container to store its data.
     * <p>
     * The directory must be created before starting the docker container, for
     * example, like this:
     *
     * <pre>
     * sudo mkdir -p /data/&lt;directory_name&gt;
     * sudo chown 12000:79 /data/&lt;directory_name&gt;
     * </pre>
     *
     * @return The path to the external directory
     */
    private String getMountsDirectory() {
      return mountsDirectory;
    }

    /**
     * Set the path to the container-external mounts directory that can be used by
     * the HANA docker image to store its data.
     *
     * @param mountsDirectory The path to the external directory
     */
    public Builder mountsDirectory(String mountsDirectory) {
      this.mountsDirectory = mountsDirectory;
      return self();
    }

    /**
     * Return the URL of the file containing the default password(s) for the HANA
     * database users.
     * <p>
     * The file must contain passwords in a JSON format, for example:
     *
     * <pre>
     * {
     *   "master_password" : "HXEHana1"
     * }
     * </pre>
     * <p>
     * If the file is located in the container-external mounts directory (see
     * {@link #getMountsDirectory()}), the URL should be
     * {@code file:///hana/mounts/<file_name>.json}
     *
     * @return The URL of the file containing the default password(s) for the HANA
     * database users.
     */
    private URL getPasswordsUrl() {
      return passwordsUrl;
    }

    /**
     * Set the URL of the file containing the default password(s) for the HANA
     * database users.
     *
     * @param passwordsUrl The URL of the file containing the default password(s)
     *                     for the HANA database users.
     */
    public Builder passwordsUrl(URL passwordsUrl) {
      this.passwordsUrl = passwordsUrl;
      return self();
    }

    /**
     * Return the container-external instance number of the HANA database.
     * <p>
     * A different instance number is necessary when running more than one instance
     * of HANA on one host. The instance number can range from 00 to 99. The default
     * instance number is 90.
     *
     * @return The container-external instance number of the HANA database.
     */
    private String getInstanceNumber() {
      return instanceNumber;
    }

    /**
     * Set the container-external instance number of the HANA database.
     *
     * @param instanceNumber The container-external instance number of the HANA
     *                       database.
     */
    public Builder instanceNumber(String instanceNumber) {
      this.instanceNumber = instanceNumber;
      return self();
    }

    /**
     * Returns whether the user agrees to the <a href=
     * "https://www.sap.com/docs/download/cmp/2016/06/sap-hana-express-dev-agmt-and-exhibit.pdf">SAP
     * license</a> for the HANA docker image.
     *
     * @return {@code true} if the user agrees to the license, {@code false}
     * otherwise.
     */
    private boolean isAgreeToSapLicense() {
      return agreeToSapLicense;
    }

    /**
     * Set whether the user agrees to the <a href=
     * "https://www.sap.com/docs/download/cmp/2016/06/sap-hana-express-dev-agmt-and-exhibit.pdf">SAP
     * license</a> for the HANA docker image.
     *
     * @param agreeToSapLicense Whether the user agrees to the license or not
     * @return
     */
    public Builder agreeToSapLicense(boolean agreeToSapLicense) {
      this.agreeToSapLicense = agreeToSapLicense;
      return self();
    }

    /**
     * Check if the user has agreed to the <a href=
     * "https://www.sap.com/docs/download/cmp/2016/06/sap-hana-express-dev-agmt-and-exhibit.pdf">SAP
     * license</a>
     *
     * @param properties The properties to check
     * @return {@code true} if the user has agreed to the license, {@code false}
     * otherwise
     */
    private boolean checkLicenseAgreementFor(Properties properties) {
      String propertyValue = null;
      if (properties != null) {
        propertyValue = prop(properties, "agreeToSapLicense", null);
        if (propertyValue != null) {
          return Boolean.parseBoolean(propertyValue);
        }
      }
      return checkLicenseAgreement();
    }
  }


  private static final Logger log = LoggerFactory.getLogger(Commands.class);

  private final Builder hanaConfig;

  /**
   * Create with configuration.
   */
  private HanaContainer(Builder builder) {
    super(builder);
    this.hanaConfig = builder;
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
    args.add(config.docker());
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
        sqlRun(connection, "drop user " + dbConfig.getUsername() + " cascade");
      }
    });
    return true;
  }

  private boolean createUserIfNotExists() {
    log.info("Create database user {} if not exists", dbConfig.getUsername());
    sqlProcess(connection -> {
      if (!userExists(connection)) {
        sqlRun(connection, "create user " + dbConfig.getUsername() + " password " + dbConfig.getPassword()
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

  @SuppressWarnings("unchecked")
  private <E extends Throwable> void sneakyThrow(Throwable t) throws E {
    throw (E) t;
  }
}
