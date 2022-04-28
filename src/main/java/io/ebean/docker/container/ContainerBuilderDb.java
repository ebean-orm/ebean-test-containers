package io.ebean.docker.container;

/**
 * Builder for DB containers.
 */
public interface ContainerBuilderDb<SELF extends ContainerBuilderDb<SELF>> extends ContainerBuilder<SELF> {

  /**
   * Set the database admin user.
   */
  SELF setAdminUser(String dbAdminUser);

  /**
   * Set the password for the database admin user.
   */
  SELF setAdminPassword(String adminPassword);

  /**
   * Set the Temp file system to use.
   */
  SELF setTmpfs(String tmpfs);

  /**
   * Set the database name. Defaults to test_db.
   */
  SELF setDbName(String dbName);

  /**
   * Set the database user.
   */
  SELF setUser(String user);

  /**
   * Set the database password.
   */
  SELF setPassword(String password);

  /**
   * Set the database schema.
   */
  SELF setSchema(String schema);

  /**
   * Set the character set.
   */
  SELF setCharacterSet(String characterSet);

  /**
   * Set the Collation.
   */
  SELF setCollation(String collation);

  /**
   * Set the database extensions to use.
   */
  SELF setExtensions(String extensions);

  /**
   * Set the init sql file to execute.
   */
  SELF setInitSqlFile(String initSqlFile);

  /**
   * Set the seed sql file to execute.
   */
  SELF setSeedSqlFile(String seedSqlFile);

  /**
   * Set an extra database to create.
   */
  SELF setExtraDb(String extraDb);

  /**
   * Set extra database user.
   */
  SELF setExtraDbUser(String extraDbUser);

  /**
   * Set extra database users password.
   */
  SELF setExtraDbPassword(String extraDbPassword);

  /**
   * Set extra database init sql file to execute.
   */
  SELF setExtraDbInitSqlFile(String extraDbInitSqlFile);

  /**
   * Set extra database seed sql file to execute.
   */
  SELF setExtraDbSeedSqlFile(String extraDbSeedSqlFile);

  /**
   * Set to true to use in-memory database if supported.
   */
  SELF setInMemory(boolean inMemory);

  /**
   * Set fast start mode.
   */
  SELF setFastStartMode(boolean fastStartMode);

}
