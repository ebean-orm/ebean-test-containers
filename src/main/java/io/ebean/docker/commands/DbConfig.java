package io.ebean.docker.commands;

import io.ebean.docker.container.ContainerBuilderDb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Configuration for an DBMS like Postgres, MySql, Oracle, SQLServer
 */
abstract class DbConfig<SELF extends DbConfig<SELF>> extends BaseConfig<SELF> implements ContainerBuilderDb<SELF> {

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

  /**
   * The character set to use.
   */
  protected String characterSet;

  /**
   * The collation to use.
   */
  protected String collation;

  DbConfig(String platform, int port, int internalPort, String version) {
    super(platform, port, internalPort, version);
  }

  protected String getDbName() {
    return dbName;
  }

  /**
   * Load configuration from properties.
   */
  @Override
  public SELF setProperties(Properties properties) {
    if (properties == null) {
      return self();
    }
    super.setProperties(properties);
    characterSet = prop(properties, "characterSet", characterSet);
    collation = prop(properties, "collation", collation);
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
    return self();
  }

  /**
   * Set the password for the DB admin user.
   */
  @Override
  public SELF setAdminUser(String dbAdminUser) {
    this.adminUsername = dbAdminUser;
    return self();
  }

  /**
   * Set the password for the DB admin user.
   */
  @Override
  public SELF setAdminPassword(String adminPassword) {
    this.adminPassword = adminPassword;
    return self();
  }

  /**
   * Set the temp fs for in-memory use.
   */
  @Override
  public SELF setTmpfs(String tmpfs) {
    this.tmpfs = tmpfs;
    return self();
  }

  /**
   * Set to true to use fast start mode.
   */
  @Override
  public SELF setFastStartMode(boolean fastStartMode) {
    this.fastStartMode = fastStartMode;
    return self();
  }

  /**
   * Set the DB name.
   */
  @Override
  public SELF setDbName(String dbName) {
    this.dbName = dbName;
    return self();
  }

  /**
   * Set the DB user.
   */
  @Override
  public SELF setUser(String user) {
    this.username = user;
    return self();
  }

  /**
   * Set the DB password.
   */
  @Override
  public SELF setPassword(String password) {
    this.password = password;
    return self();
  }

  /**
   * Set the DB schema.
   */
  @Override
  public SELF setSchema(String schema) {
    this.schema = schema;
    return self();
  }

  /**
   * Set the character set to use.
   */
  @Override
  public SELF setCharacterSet(String characterSet) {
    this.characterSet = characterSet;
    return self();
  }

  /**
   * Set the collation to use.
   */
  @Override
  public SELF setCollation(String collation) {
    this.collation = collation;
    return self();
  }

  /**
   * Set the DB extensions to install (Postgres hstore, pgcrypto etc)
   */
  @Override
  public SELF setExtensions(String extensions) {
    this.extensions = extensions;
    return self();
  }

  /**
   * Set the SQL file to execute after creating the database.
   */
  @Override
  public SELF setInitSqlFile(String initSqlFile) {
    this.initSqlFile = initSqlFile;
    return self();
  }

  /**
   * Set the SQL file to execute after creating the database and initSqlFile.
   */
  @Override
  public SELF setSeedSqlFile(String seedSqlFile) {
    this.seedSqlFile = seedSqlFile;
    return self();
  }

  /**
   * Set the name of an extra database to create.
   */
  @Override
  public SELF setExtraDb(String extraDb) {
    this.extraDb = extraDb;
    return self();
  }

  /**
   * Set the name of an extra user to create. If an extra database is also created this would be the
   * owner of that extra database.
   */
  @Override
  public SELF setExtraDbUser(String extraDbUser) {
    this.extraDbUser = extraDbUser;
    return self();
  }

  /**
   * Set the password for an extra user. If nothing is set this would default to be the same as
   * the main users password.
   */
  @Override
  public SELF setExtraDbPassword(String extraDbPassword) {
    this.extraDbPassword = extraDbPassword;
    return self();
  }

  /**
   * Set a file to execute after creating the extra database.
   */
  @Override
  public SELF setExtraDbInitSqlFile(String extraDbInitSqlFile) {
    this.extraDbInitSqlFile = extraDbInitSqlFile;
    return self();
  }

  /**
   * Set a file to execute after creating the extra database.
   */
  @Override
  public SELF setExtraDbSeedSqlFile(String extraDbSeedSqlFile) {
    this.extraDbSeedSqlFile = extraDbSeedSqlFile;
    return self();
  }

  /**
   * Set to true to run using in memory storage for data via tmpfs.
   */
  @Override
  public SELF setInMemory(boolean inMemory) {
    this.inMemory = inMemory;
    return self();
  }

  /**
   * Set the schema if it hasn't already set. Some databases (NuoDB) effectively require a
   * default schema and it is reasonable for this to default to the username.
   */
  protected void initDefaultSchema() {
    if (schema == null) {
      schema = username;
    }
  }

  /**
   * Return summary of the port db name and other details.
   */
  protected String buildSummary() {
    return "host:" + host + " port:" + port + " db:" + dbName + " user:" + username + "/" + password;
  }

  @Override
  protected InternalConfigDb internalConfig() {
    return new InnerConfig();
  }

  private class InnerConfig extends BaseConfig<?>.Inner implements InternalConfigDb {

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
    @Override
    public String summary() {
      return buildSummary();
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
    @Override
    public Connection createAdminConnection(String url) throws SQLException {
      Properties props = new java.util.Properties();
      props.put("user", adminUsername);
      props.put("password", adminPassword);
      return DriverManager.getConnection(url, props);
    }

    @Override
    public Connection createAdminConnection() throws SQLException {
      return createAdminConnection(jdbcAdminUrl());
    }

    @Override
    public boolean isInMemory() {
      return inMemory;
    }

    @Override
    public String getTmpfs() {
      return tmpfs;
    }

    @Override
    public String getAdminUsername() {
      return adminUsername;
    }

    @Override
    public String getAdminPassword() {
      return adminPassword;
    }

    @Override
    public String getDbName() {
      return dbName;
    }

    @Override
    public String getUsername() {
      return username;
    }

    @Override
    public String getPassword() {
      return password;
    }

    @Override
    public String getSchema() {
      return schema;
    }

    @Override
    public String getExtensions() {
      return extensions;
    }

    @Override
    public String getInitSqlFile() {
      return initSqlFile;
    }

    @Override
    public String getSeedSqlFile() {
      return seedSqlFile;
    }

    @Override
    public String getExtraDb() {
      return extraDb;
    }

    @Override
    public String getExtraDbUser() {
      return extraDbUser;
    }

    @Override
    public String getExtraDbPassword() {
      return extraDbPassword;
    }

    @Override
    public String getExtraDbInitSqlFile() {
      return extraDbInitSqlFile;
    }

    @Override
    public String getExtraDbSeedSqlFile() {
      return extraDbSeedSqlFile;
    }

    @Override
    public boolean isFastStartMode() {
      return fastStartMode;
    }

    @Override
    public String getCharacterSet() {
      return characterSet;
    }

    @Override
    public String getCollation() {
      return collation;
    }

    @Override
    public boolean isExplicitCollation() {
      return collation != null || characterSet != null;
    }

    @Override
    public boolean isDefaultCollation() {
      return "default".equals(collation);
    }
  }
}
