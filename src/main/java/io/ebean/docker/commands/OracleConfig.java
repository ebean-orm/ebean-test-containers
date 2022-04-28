package io.ebean.docker.commands;

import java.util.Properties;

/**
 * Oracle configuration.
 */
public class OracleConfig extends DbConfig {

  private String apexPort = "8181";

  private String internalApexPort = "8080";

  /**
   * Wait time allowed when starting oracle from scratch.
   */
  private int startupWaitMinutes = 8;

  public OracleConfig(String version, Properties properties) {
    this(version);
    setProperties(properties);
  }

  public OracleConfig() {
    this("latest");
  }

  public OracleConfig(String version) {
    super("oracle", 1521, 1521, version);
    this.image = "vitorfec/oracle-xe-18c:" + version;
    setAdminUser("system");
    setAdminPassword("oracle");
    setDbName("XE");
  }

  @Override
  protected String buildJdbcUrl() {
    return "jdbc:oracle:thin:@" + getHost() + ":" + getPort() + ":" + getDbName();
  }

  public String getApexPort() {
    return apexPort;
  }

  public void setApexPort(String apexPort) {
    this.apexPort = apexPort;
  }

  public String getInternalApexPort() {
    return internalApexPort;
  }

  public void setInternalApexPort(String internalApexPort) {
    this.internalApexPort = internalApexPort;
  }

  public int getStartupWaitMinutes() {
    return startupWaitMinutes;
  }

  public void setStartupWaitMinutes(int startupWaitMinutes) {
    this.startupWaitMinutes = startupWaitMinutes;
  }
}
