package io.ebean.docker.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * SAP HANA configuration.
 * <p>
 * For more information about the HANA docker configuration see the tutorial
 * <a href="https://developers.sap.com/tutorials/hxe-ua-install-using-docker.html">Installing SAP HANA, express edition with Docker</a>
 */
public class HanaConfig extends DbConfig<HanaContainer, HanaConfig> {

  private static final Logger log = LoggerFactory.getLogger(HanaConfig.class);

  private String mountsDirectory;
  private URL passwordsUrl;
  private String instanceNumber;
  private boolean agreeToSapLicense;

  public HanaConfig(String version, Properties properties) {
    this(version);
    properties(properties);
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
    this.agreeToSapLicense = checkLicenseAgreement(properties);
  }

  public HanaConfig(String version) {
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
  public String getMountsDirectory() {
    return mountsDirectory;
  }

  /**
   * Set the path to the container-external mounts directory that can be used by
   * the HANA docker image to store its data.
   *
   * @param mountsDirectory The path to the external directory
   */
  public void setMountsDirectory(String mountsDirectory) {
    this.mountsDirectory = mountsDirectory;
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
  public URL getPasswordsUrl() {
    return passwordsUrl;
  }

  /**
   * Set the URL of the file containing the default password(s) for the HANA
   * database users.
   *
   * @param passwordsUrl The URL of the file containing the default password(s)
   *                     for the HANA database users.
   */
  public void setPasswordsUrl(URL passwordsUrl) {
    this.passwordsUrl = passwordsUrl;
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
  public String getInstanceNumber() {
    return instanceNumber;
  }

  /**
   * Set the container-external instance number of the HANA database.
   *
   * @param instanceNumber The container-external instance number of the HANA
   *                       database.
   */
  public void setInstanceNumber(String instanceNumber) {
    this.instanceNumber = instanceNumber;
  }

  /**
   * Returns whether the user agrees to the <a href=
   * "https://www.sap.com/docs/download/cmp/2016/06/sap-hana-express-dev-agmt-and-exhibit.pdf">SAP
   * license</a> for the HANA docker image.
   *
   * @return {@code true} if the user agrees to the license, {@code false}
   * otherwise.
   */
  public boolean isAgreeToSapLicense() {
    return agreeToSapLicense;
  }

  /**
   * Set whether the user agrees to the <a href=
   * "https://www.sap.com/docs/download/cmp/2016/06/sap-hana-express-dev-agmt-and-exhibit.pdf">SAP
   * license</a> for the HANA docker image.
   *
   * @param agreeToSapLicense Whether the user agrees to the license or not
   */
  public void setAgreeToSapLicense(boolean agreeToSapLicense) {
    this.agreeToSapLicense = agreeToSapLicense;
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
  public boolean checkLicenseAgreement(Properties properties) {
    String propertyValue = null;
    if (properties != null) {
      propertyValue = prop(properties, "agreeToSapLicense", null);
      if (propertyValue != null) {
        return Boolean.parseBoolean(propertyValue);
      }
    }

    return checkLicenseAgreement();
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


}
