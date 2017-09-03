package org.avaje.docker.commands.postgres;

public class PostgresConfig {

  /**
   * Container name.
   */
  public String name = "ut_postgres";

  /**
   * The exposed port.
   */
  public String hostPort = "6432";

  /**
   * The internal port.
   */
  public String pgPort = "5432";

  /**
   * Postgres admin password.
   */
  public String pgPassword = "admin";

  /**
   * Set for in-memory tmpfs use.
   */
  public String tmpfs = "/var/lib/postgresql/data:rw";

  /**
   * Image name.
   */
  public String image = "postgres:9.5.4";

  /**
   * Database name to use.
   */
  public String dbName = "test_db";

  /**
   * Database user to use.
   */
  public String dbUser = "test_user";

  /**
   * Database password for the user.
   */
  public String dbPassword = "test";

  /**
   * Comma delimited list of database extensions required (hstore, pgcrypto etc).
   */
  public String dbExtensions;

  /**
   * Maximum number of attempts to find the 'database ready to accept connections' log message in the container.
   * <p>
   * 50 attempts equates to 5 seconds.
   * </p>
   */
  public int maxLogReadyAttempts = 50;

  /**
   * Docker command.
   */
  public String docker = "docker";


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
