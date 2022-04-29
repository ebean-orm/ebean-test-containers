package io.ebean.docker.container;

/**
 * Builder for DB containers.
 */
public interface ContainerBuilderDb<C,SELF extends ContainerBuilderDb<C,SELF>> extends ContainerBuilder<C,SELF> {

  /**
   * Set the database admin user.
   */
  SELF adminUser(String dbAdminUser);

  /**
   * Set the password for the database admin user.
   */
  SELF adminPassword(String adminPassword);

  /**
   * Set the Temp file system to use.
   */
  SELF tmpfs(String tmpfs);

  /**
   * Set the database name. Defaults to test_db.
   */
  SELF dbName(String dbName);

  /**
   * Set the database user.
   */
  SELF user(String user);

  /**
   * Set the database password.
   */
  SELF password(String password);

  /**
   * Set the database schema.
   */
  SELF schema(String schema);

  /**
   * Set the character set.
   */
  SELF characterSet(String characterSet);

  /**
   * Set the Collation.
   */
  SELF collation(String collation);

  /**
   * Set the database extensions to use.
   */
  SELF extensions(String extensions);

  /**
   * Set the init sql file to execute.
   */
  SELF initSqlFile(String initSqlFile);

  /**
   * Set the seed sql file to execute.
   */
  SELF seedSqlFile(String seedSqlFile);

  /**
   * Set an extra database to create.
   */
  SELF extraDb(String extraDb);

  /**
   * Set extra database user.
   */
  SELF extraDbUser(String extraDbUser);

  /**
   * Set extra database users password.
   */
  SELF extraDbPassword(String extraDbPassword);

  /**
   * Set extra database init sql file to execute.
   */
  SELF extraDbInitSqlFile(String extraDbInitSqlFile);

  /**
   * Set extra database seed sql file to execute.
   */
  SELF extraDbSeedSqlFile(String extraDbSeedSqlFile);

  /**
   * Set to true to use in-memory database if supported.
   */
  SELF inMemory(boolean inMemory);

  /**
   * Set fast start mode.
   */
  SELF fastStartMode(boolean fastStartMode);

}
