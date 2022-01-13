package io.ebean.docker.commands;

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
  String adminUsername = "admin";

  /**
   * Database admin password.
   */
  String adminPassword = "admin";

  /**
   * An additional database.
   */
  private String extraDb;
  private String extraDbUser;
  private String extraDbPassword;
  private String extraDbInitSqlFile;
  private String extraDbSeedSqlFile;

  /**
   * Database name to use.
   */
  String dbName = "test_db";

  /**
   * Database user to use.
   */
  String username = "test_user";

  /**
   * Database password for the user.
   */
  String password = "test";

  /**
   * The default database schema to use if specified.
   */
  String schema;

  /**
   * Comma delimited list of database extensions required (hstore, pgcrypto etc).
   */
  private String extensions;

  /**
   * SQL file executed against the database after it has been created.
   */
  private String initSqlFile;

  /**
   * SQL file executed against the database after initSqlFile.
   */
  private String seedSqlFile;

  /**
   * Set to true to run in-memory mode.
   */
  private boolean inMemory;

  /**
   * If true we ONLY check the existence of the DB and if present we skip the
   * other usual checks (does user exist, create extensions if not exists etc).
   */
  boolean fastStartMode = true;

  DbConfig(String platform, int port, int internalPort, String version) {
    super(platform, port, internalPort, version);
  }

  /**
   * Return a description of the configuration.
   */
  @Override
  public String startDescription() {
    return "starting " + platform + " container:" + containerName + " port:" + port + " db:" + dbName + " user:" + username + " extensions:" + extensions + " startMode:" + startMode;
  }

  /**
   * Return summary of the port db name and other details.
   */
  public String summary() {
    return "host:" + host + " port:" + port + " db:" + dbName + " user:" + username + "/" + password;
  }

  /**
   * Set the schema if it hasn't already set. Some databases (NuoDB) effectively require a
   * default schema and it is reasonable for this to default to the username.
   */
  public void initDefaultSchema() {
    if (schema == null) {
      schema = username;
    }
  }

  /**
   * Return a Connection to the database (make sure you close it).
   */
  @Override
  public Connection createConnection() throws SQLException {
    Properties props = new java.util.Properties();
    props.put("user", username);
    props.put("password", password);
    if (schema != null) {
      props.put("schema", schema);
    }
    return DriverManager.getConnection(jdbcUrl(), props);
  }

  @Override
  public Connection createConnectionNoSchema() throws SQLException {
    Properties props = new java.util.Properties();
    props.put("user", username);
    props.put("password", password);
    return DriverManager.getConnection(jdbcUrl(), props);
  }

  /**
   * Return a Connection to the database using the admin user.
   */
  public Connection createAdminConnection(String url) throws SQLException {
    Properties props = new java.util.Properties();
    props.put("user", adminUsername);
    props.put("password", adminPassword);
    return DriverManager.getConnection(url, props);
  }

  public Connection createAdminConnection() throws SQLException {
    return createAdminConnection(jdbcAdminUrl());
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
    fastStartMode = Boolean.parseBoolean(prop(properties, "fastStartMode", Boolean.toString(fastStartMode)));

    tmpfs = prop(properties, "tmpfs", tmpfs);
    dbName = prop(properties, "dbName", dbName);
    username = prop(properties, "username", username);
    password = prop(properties, "password", password);
    schema = prop(properties, "schema", schema);
    extensions = prop(properties, "extensions", extensions);
    adminUsername = prop(properties, "adminUsername", adminUsername);
    adminPassword = prop(properties, "adminPassword", adminPassword);
    initSqlFile = prop(properties, "initSqlFile", initSqlFile);
    seedSqlFile = prop(properties, "seedSqlFile", seedSqlFile);

    extraDb = prop(properties, "extraDb.dbName", prop(properties, "extraDb", extraDb));
    extraDbUser = prop(properties, "extraDb.username", extraDbUser);
    extraDbPassword = prop(properties, "extraDb.password", extraDbPassword);
    extraDbInitSqlFile = prop(properties, "extraDb.initSqlFile", extraDbInitSqlFile);
    extraDbSeedSqlFile = prop(properties, "extraDb.seedSqlFile", extraDbSeedSqlFile);
    return this;
  }

  /**
   * Set the password for the DB admin user.
   */
  public DbConfig setAdminUser(String dbAdminUser) {
    this.adminUsername = dbAdminUser;
    return this;
  }

  /**
   * Set the password for the DB admin user.
   */
  public DbConfig setAdminPassword(String adminPassword) {
    this.adminPassword = adminPassword;
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
   * Set to true to use fast start mode.
   */
  public DbConfig setFastStartMode(boolean fastStartMode) {
    this.fastStartMode = fastStartMode;
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
    this.username = user;
    return this;
  }

  /**
   * Set the DB password.
   */
  public DbConfig setPassword(String password) {
    this.password = password;
    return this;
  }

  /**
   * Set the DB schema.
   */
  public DbConfig setSchema(String schema) {
    this.schema = schema;
    return this;
  }

  /**
   * Set the DB extensions to install (Postgres hstore, pgcrypto etc)
   */
  public DbConfig setExtensions(String extensions) {
    this.extensions = extensions;
    return this;
  }

  /**
   * Set the SQL file to execute after creating the database.
   */
  public DbConfig setInitSqlFile(String initSqlFile) {
    this.initSqlFile = initSqlFile;
    return this;
  }

  /**
   * Set the SQL file to execute after creating the database and initSqlFile.
   */
  public DbConfig setSeedSqlFile(String seedSqlFile) {
    this.seedSqlFile = seedSqlFile;
    return this;
  }

  /**
   * Set the name of an extra database to create.
   */
  public DbConfig setExtraDb(String extraDb) {
    this.extraDb = extraDb;
    return this;
  }

  /**
   * Set the name of an extra user to create. If an extra database is also created this would be the
   * owner of that extra database.
   */
  public DbConfig setExtraDbUser(String extraDbUser) {
    this.extraDbUser = extraDbUser;
    return this;
  }

  /**
   * Set the password for an extra user. If nothing is set this would default to be the same as
   * the main users password.
   */
  public DbConfig setExtraDbPassword(String extraDbPassword) {
    this.extraDbPassword = extraDbPassword;
    return this;
  }

  /**
   * Set a file to execute after creating the extra database.
   */
  public DbConfig setExtraDbInitSqlFile(String extraDbInitSqlFile) {
    this.extraDbInitSqlFile = extraDbInitSqlFile;
    return this;
  }

  /**
   * Set a file to execute after creating the extra database.
   */
  public DbConfig setExtraDbSeedSqlFile(String extraDbSeedSqlFile) {
    this.extraDbSeedSqlFile = extraDbSeedSqlFile;
    return this;
  }

  /**
   * Set to true to run using in memory storage for data via tmpfs.
   */
  public DbConfig setInMemory(boolean inMemory) {
    this.inMemory = inMemory;
    return this;
  }

  public boolean isInMemory() {
    return inMemory;
  }

  public String getTmpfs() {
    return tmpfs;
  }

  public String getAdminUsername() {
    return adminUsername;
  }

  public String getAdminPassword() {
    return adminPassword;
  }

  public String getDbName() {
    return dbName;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getSchema() {
    return schema;
  }

  public String getExtensions() {
    return extensions;
  }

  public String getInitSqlFile() {
    return initSqlFile;
  }

  public String getSeedSqlFile() {
    return seedSqlFile;
  }

  public String getExtraDb() {
    return extraDb;
  }

  public String getExtraDbUser() {
    return extraDbUser;
  }

  public String getExtraDbPassword() {
    return extraDbPassword;
  }

  public String getExtraDbInitSqlFile() {
    return extraDbInitSqlFile;
  }

  public String getExtraDbSeedSqlFile() {
    return extraDbSeedSqlFile;
  }

  public boolean isFastStartMode() {
    return fastStartMode;
  }
}
