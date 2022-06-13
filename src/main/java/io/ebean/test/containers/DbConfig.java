package io.ebean.test.containers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Configuration for an DBMS like Postgres, MySql, Oracle, SQLServer
 */
abstract class DbConfig<C, SELF extends DbConfig<C, SELF>> extends BaseConfig<C, SELF> implements ContainerBuilderDb<C, SELF> {

  /**
   * Set for in-memory tmpfs use.
   */
  String tmpfs;

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
  String extraDb;
  private String extraDbUser;
  private String extraDbPassword;
  private String extraDbExtensions;
  private String extraDbInitSqlFile;
  private String extraDbSeedSqlFile;

  /**
   * Database name to use.
   */
  String dbName = "test_db";

  /**
   * Database user to use. Defaults to be the same as dbName.
   */
  String username;

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
  public SELF properties(Properties properties) {
    if (properties == null) {
      return self();
    }
    super.properties(properties);
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
    extraDbExtensions = prop(properties, "extraDb.extensions", extraDbExtensions);
    extraDbInitSqlFile = prop(properties, "extraDb.initSqlFile", extraDbInitSqlFile);
    extraDbSeedSqlFile = prop(properties, "extraDb.seedSqlFile", extraDbSeedSqlFile);
    return self();
  }

  /**
   * Set the password for the DB admin user.
   */
  @Override
  public SELF adminUser(String dbAdminUser) {
    this.adminUsername = dbAdminUser;
    return self();
  }

  /**
   * Set the password for the DB admin user.
   */
  @Override
  public SELF adminPassword(String adminPassword) {
    this.adminPassword = adminPassword;
    return self();
  }

  /**
   * Set the temp fs for in-memory use.
   */
  @Override
  public SELF tmpfs(String tmpfs) {
    this.tmpfs = tmpfs;
    return self();
  }

  /**
   * Defaults to true - If true ONLY check the existence of the DB and if present
   * skip the other usual checks (does user exist, create extensions if not exists etc).
   */
  @Override
  public SELF fastStartMode(boolean fastStartMode) {
    this.fastStartMode = fastStartMode;
    return self();
  }

  /**
   * Set the DB name - e.g. my_app1, my_app2, my_app3 etc. Defaults to test_db.
   * <p>
   * The DB name should not have any special characters (alpha and underscore) and
   * should be unique for the project.
   * <p>
   * ebean-test-docker is designed to share the same container across multiple projects. The
   * way this works is that each project should have a unique db name. This means that as
   * developers testing is faster as containers stay running and are shared and running
   * tests for a project means setting up unique "database" using the dbName.
   */
  @Override
  public SELF dbName(String dbName) {
    this.dbName = dbName;
    return self();
  }

  /**
   * Set the DB user. Defaults to being the same as the dbName.
   */
  @Override
  public SELF user(String user) {
    this.username = user;
    return self();
  }

  /**
   * Set the DB password. Defaults to test.
   */
  @Override
  public SELF password(String password) {
    this.password = password;
    return self();
  }

  /**
   * Set the DB schema.
   */
  @Override
  public SELF schema(String schema) {
    this.schema = schema;
    return self();
  }

  /**
   * Set the character set to use.
   */
  @Override
  public SELF characterSet(String characterSet) {
    this.characterSet = characterSet;
    return self();
  }

  /**
   * Set the collation to use.
   */
  @Override
  public SELF collation(String collation) {
    this.collation = collation;
    return self();
  }

  /**
   * Set the DB extensions to install in comma delimited form.
   * <p>
   * Postgres hstore, pgcrypto etc.
   * <p>
   * Example:  {@code .extensions("hstore,pgcrypto,uuid-ossp") }
   */
  @Override
  public SELF extensions(String extensions) {
    this.extensions = extensions;
    return self();
  }

  /**
   * Set the SQL file to execute after creating the database.
   */
  @Override
  public SELF initSqlFile(String initSqlFile) {
    this.initSqlFile = initSqlFile;
    return self();
  }

  /**
   * Set the SQL file to execute after creating the database and initSqlFile.
   */
  @Override
  public SELF seedSqlFile(String seedSqlFile) {
    this.seedSqlFile = seedSqlFile;
    return self();
  }

  /**
   * Set the name of an extra database to create.
   * <p>
   * Use this when the application being tested uses 2 databases.
   */
  @Override
  public SELF extraDb(String extraDb) {
    this.extraDb = extraDb;
    return self();
  }

  /**
   * Set the name of an extra user to create. If an extra database is also created this would be the
   * owner of that extra database.
   * <p>
   * Use this when the application being tested uses 2 databases.
   */
  @Override
  public SELF extraDbUser(String extraDbUser) {
    this.extraDbUser = extraDbUser;
    return self();
  }

  /**
   * Set the password for an extra user. If nothing is set this would default to be the same as
   * the main users password.
   * <p>
   * Use this when the application being tested uses 2 databases.
   */
  @Override
  public SELF extraDbPassword(String extraDbPassword) {
    this.extraDbPassword = extraDbPassword;
    return self();
  }

  @Override
  public SELF extraDbExtensions(String extraDbExtensions) {
    this.extraDbExtensions = extraDbExtensions;
    return self();
  }

  /**
   * Set a file to execute after creating the extra database.
   */
  @Override
  public SELF extraDbInitSqlFile(String extraDbInitSqlFile) {
    this.extraDbInitSqlFile = extraDbInitSqlFile;
    return self();
  }

  /**
   * Set a file to execute after creating the extra database.
   */
  @Override
  public SELF extraDbSeedSqlFile(String extraDbSeedSqlFile) {
    this.extraDbSeedSqlFile = extraDbSeedSqlFile;
    return self();
  }

  /**
   * Set to true to run using in memory storage for data via tmpfs.
   */
  @Override
  public SELF inMemory(boolean inMemory) {
    this.inMemory = inMemory;
    return self();
  }

  /**
   * Set the schema if it hasn't already set. Some databases (NuoDB) effectively require a
   * default schema and it is reasonable for this to default to the username.
   */
  protected void initDefaultSchema() {
    if (schema == null) {
      schema = deriveUsername();
    }
  }

  /**
   * Return summary of the port db name and other details.
   */
  protected String buildSummary() {
    return "host:" + host + " port:" + port + " db:" + dbName + " user:" + deriveUsername() + "/" + password;
  }

  protected String deriveUsername() {
    return username == null ? dbName : username;
  }

  @Override
  protected InternalConfigDb internalConfig() {
    return new InnerConfig();
  }

  private class InnerConfig extends BaseConfig<?, ?>.Inner implements InternalConfigDb {

    /**
     * Return a description of the configuration.
     */
    @Override
    public String startDescription() {
      return "starting " + platform + " container:" + containerName + " port:" + port + " db:" + dbName + " user:" + deriveUsername() + " extensions:" + extensions + " startMode:" + startMode;
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
      props.put("user", getUsername());
      props.put("password", getPassword());
      if (schema != null) {
        props.put("schema", getSchema());
      }
      return DriverManager.getConnection(jdbcUrl(), props);
    }

    @Override
    public Connection createConnectionNoSchema() throws SQLException {
      Properties props = new java.util.Properties();
      props.put("user", getUsername());
      props.put("password", getPassword());
      return DriverManager.getConnection(jdbcUrl(), props);
    }

    /**
     * Return a Connection to the database using the admin user.
     */
    @Override
    public Connection createAdminConnection(String url) throws SQLException {
      Properties props = new java.util.Properties();
      props.put("user", getAdminUsername());
      props.put("password", getAdminPassword());
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
      return deriveUsername();
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
    public String getExtraDbExtensions() {
      return extraDbExtensions;
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
