package io.ebean.test.containers;

/**
 * Builder for attributes on an extra database.
 */
public interface ExtraBuilder {

  /**
   * Set the database name.
   */
  ExtraBuilder dbName(String dbName);

  /**
   * Set the extensions.
   */
  ExtraBuilder extensions(String extensions);

  /**
   * Set the username.
   */
  ExtraBuilder username(String username);

  /**
   * Set the password.
   */
  ExtraBuilder password(String password);

  /**
   * Set the initSqlFile.
   */
  ExtraBuilder initSqlFile(String initSqlFile);

  /**
   * Set the seedSqlFile.
   */
  ExtraBuilder seedSqlFile(String seedSqlFile);
}
