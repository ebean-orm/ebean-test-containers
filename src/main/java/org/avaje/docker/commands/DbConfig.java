package org.avaje.docker.commands;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Configuration for an DBMS like Postgres, MySql, Oracle, SQLServer
 */
public abstract class DbConfig extends BaseConfig {

  /**
   * Set for in-memory tmpfs use.
   */
  private String tmpfs;

  /**
   * Database admin password.
   */
  private String dbAdminUser = "admin";

  /**
   * Database admin password.
   */
  private String dbAdminPassword = "admin";

  /**
   * Database name to use.
   */
  private String dbName = "test_db";

  /**
   * Database user to use.
   */
  private String dbUser = "test_user";

  /**
   * Database password for the user.
   */
  private String dbPassword = "test";

  /**
   * Comma delimited list of database extensions required (hstore, pgcrypto etc).
   */
  private String dbExtensions;

  /**
   * Set to true to run in-memory mode.
   */
  private boolean inMemory;


  DbConfig(String platform, String port, String internalPort, String version) {
    super(platform, port, internalPort, version);
  }

  /**
   * Return a description of the configuration.
   */
  @Override
  public String startDescription() {
    return "starting " + platform + " container:" + containerName + " port:" + port + " db:" + dbName + " user:" + dbUser + " extensions:" + dbExtensions + " startMode:" + startMode;
  }

  /**
   * Return a Connection to the database (make sure you close it).
   */
  @Override
  public Connection createConnection() throws SQLException {
    return DriverManager.getConnection(jdbcUrl(), getDbUser(), getDbPassword());
  }

  /**
   * Return a Connection to the database using the admin user.
   */
  public Connection createAdminConnection() throws SQLException {
    return DriverManager.getConnection(jdbcUrl(), getDbAdminUser(), getDbAdminPassword());
  }

  /**
   * Load configuration from properties.
   */
  public DbConfig setProperties(Properties properties) {
    if (properties == null) {
      return this;
    }
    super.setProperties(properties);

    inMemory = Boolean.parseBoolean(prop(properties, "inMemory", Boolean.toString(inMemory)));
    tmpfs = prop(properties, "tmpfs", tmpfs);
    dbName = prop(properties, "dbName", dbName);
    dbUser = prop(properties, "dbUser", dbUser);
    dbPassword = prop(properties, "dbPassword", dbPassword);
    dbExtensions = prop(properties, "dbExtensions", dbExtensions);
    dbAdminPassword = prop(properties, "dbAdminPassword", dbAdminPassword);

    return this;
  }

  /**
   * Set the password for the DB admin user.
   */
  public DbConfig setAdminUser(String dbAdminUser) {
    this.dbAdminUser= dbAdminUser;
    return this;
  }

  /**
   * Set the password for the DB admin user.
   */
  public DbConfig setAdminPassword(String adminPassword) {
    this.dbAdminPassword = adminPassword;
    return this;
  }

  /**
   * Set the temp fs for in-memory use.
   */
  public DbConfig setTmpfs(String tmpfs) {
    this.tmpfs = tmpfs;
    return this;
  }

  /**
   * Set the DB name.
   */
  public DbConfig setDbName(String dbName) {
    this.dbName = dbName;
    return this;
  }

  /**
   * Set the DB user.
   */
  public DbConfig setUser(String user) {
    this.dbUser = user;
    return this;
  }

  /**
   * Set the DB password.
   */
  public DbConfig setPassword(String password) {
    this.dbPassword = password;
    return this;
  }

  /**
   * Set the DB extensions to install (Postgres hstore, pgcrypto etc)
   */
  public DbConfig withExtensions(String extensions) {
    this.dbExtensions = extensions;
    return this;
  }

  public boolean isInMemory() {
    return inMemory;
  }

  public String getTmpfs() {
    return tmpfs;
  }

  public String getDbAdminUser() {
    return dbAdminUser;
  }

  public String getDbAdminPassword() {
    return dbAdminPassword;
  }

  public String getDbName() {
    return dbName;
  }

  public String getDbUser() {
    return dbUser;
  }

  public String getDbPassword() {
    return dbPassword;
  }

  public String getDbExtensions() {
    return dbExtensions;
  }

}
