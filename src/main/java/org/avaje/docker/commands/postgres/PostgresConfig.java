package org.avaje.docker.commands.postgres;

public class PostgresConfig {

  /**
   * Container name.
   */
  String name = "ut_postgres";

  /**
   * The exposed port.
   */
  String hostPort = "6432";

  /**
   * The internal port.
   */
  String pgPort = "5432";

  /**
   * Postgres admin password.
   */
  String pgPassword = "admin";

  /**
   * Set for in-memory tmpfs use.
   */
  String tmpfs = "/var/lib/postgresql/data:rw";

  /**
   * Image name.
   */
  String image = "postgres:9.5.4";

  /**
   * Database name to use.
   */
  String dbName = "test_db";

  /**
   * Database user to use.
   */
  String dbUser = "test_user";

  /**
   * Database password for the user.
   */
  String dbPassword = "test";

  /**
   * Comma delimited list of database extensions required (hstore, pgcrypto etc).
   */
  String dbExtensions;

  /**
   * Maximum number of attempts to find the 'database ready to accept connections' log message in the container.
   * <p>
   * 50 attempts equates to 5 seconds.
   * </p>
   */
  int maxLogReadyAttempts = 50;

  /**
   * Docker command.
   */
  String docker = "docker";


  public PostgresConfig withName(String name) {
    this.name = name;
    return this;
  }

  public PostgresConfig withHostPort(String hostPort) {
    this.hostPort = hostPort;
    return this;
  }

  public PostgresConfig withPgPort(String pgPort) {
    this.pgPort = pgPort;
    return this;
  }

  public PostgresConfig withPgPassword(String pgPassword) {
    this.pgPassword = pgPassword;
    return this;
  }

  public PostgresConfig withTmpfs(String tmpfs) {
    this.tmpfs = tmpfs;
    return this;
  }

  public PostgresConfig withImage(String image) {
    this.image = image;
    return this;
  }

  public PostgresConfig withDbName(String dbName) {
    this.dbName = dbName;
    return this;
  }

  public PostgresConfig withDbUser(String dbUser) {
    this.dbUser = dbUser;
    return this;
  }

  public PostgresConfig withDbPassword(String dbPassword) {
    this.dbPassword = dbPassword;
    return this;
  }

  public PostgresConfig withDbExtensions(String dbExtensions) {
    this.dbExtensions = dbExtensions;
    return this;
  }

  public PostgresConfig withMaxLogReadyAttempts(int maxLogReadyAttempts) {
    this.maxLogReadyAttempts = maxLogReadyAttempts;
    return this;
  }

  public PostgresConfig withDocker(String docker) {
    this.docker = docker;
    return this;
  }
}
