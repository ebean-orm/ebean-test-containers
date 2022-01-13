package io.ebean.docker.commands;

import java.util.Properties;

public class Db2Config extends DbConfig {

  private String createOptions;
  private String configOptions;

  public Db2Config(String version, Properties properties) {
    this(version);
    setProperties(properties);
  }

  public Db2Config(String version) {
    super("db2", 50000, 50000, version);
    this.image = "ibmcom/db2:" + version;
    this.setTmpfs("/database:rw");
  }

  /**
   * Expose for MariaDB config.
   */
  protected Db2Config(String platform, int port, int internalPort, String version) {
    super(platform, port, internalPort, version);
  }

  @Override
  public String jdbcUrl() {
    return "jdbc:db2://" + getHost() + ":" + getPort() + "/" + getDbName();
  }

  public String getCreateOptions() {
    return createOptions;
  }

  /**
   * Sets additional create options specified in
   * https://www.ibm.com/docs/en/db2/11.5?topic=commands-create-database Example:
   * 'USING CODESET UTF-8 TERRITORY DE COLLATE USING IDENTITY PAGESIZE 32768'
   */
  public void setCreateOptions(String createOptions) {
    this.createOptions = createOptions;
  }

  public String getConfigOptions() {
    return configOptions;
  }

  /**
   * Sets DB2 config options. See
   * https://www.ibm.com/docs/en/db2/11.5?topic=commands-update-database-configuration
   * for details Example 'USING STRING_UNITS CODEUNITS32
   */
  public void setConfigOptions(String configOptions) {
    this.configOptions = configOptions;
  }

  /**
   * Load configuration from properties.
   */
  public DbConfig setProperties(Properties properties) {
    if (properties == null) {
      return this;
    }
    super.setProperties(properties);
    createOptions = prop(properties, "createOptions", createOptions);
    configOptions = prop(properties, "configOptions", configOptions);
    return this;
  }

}
