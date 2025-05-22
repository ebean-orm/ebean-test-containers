package io.ebean.test.containers;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

/**
 * Commands for controlling a postgis docker container.
 */
public class PostgisContainer extends BasePostgresContainer<PostgisContainer> {

  @Override
  public PostgisContainer start() {
    startOrThrow();
    return this;
  }

  /**
   * Create a builder for PostgisContainer.
   */
  public static Builder builder(String version) {
    return new Builder(version);
  }

  /**
   * Deprecated - migrate to builder().
   */
  @Deprecated
  public static Builder newBuilder(String version) {
    return builder(version);
  }

  private PostgisContainer(Builder config) {
    super(config);
  }

  /**
   * Builder for Postgis container.
   */
  public static class Builder extends BaseDbBuilder<PostgisContainer, Builder> {

    private boolean useLW;

    private Builder(String version) {
      super("postgis", 6432, 5432, version);
      this.image = "ghcr.io/baosystems/postgis:" + version;
      this.adminUsername = "postgres";
      this.tmpfs = "/var/lib/postgresql/data:rw";
      this.extensions = "hstore,pgcrypto,postgis";
      this.extra.extensions = extensions;
      this.extra2.extensions = extensions;
    }

    private String prefix() {
      return useLW ? "jdbc:postgresql_lwgis://" : "jdbc:postgresql://";
    }

    @Override
    protected String buildJdbcUrl() {
      return prefix() + host + ":" + port + "/" + dbName;
    }

    @Override
    protected String buildJdbcAdminUrl() {
      return prefix() + host + ":" + port + "/postgres";
    }

    @Override
    protected String buildExtraJdbcUrl(String dbName) {
      return prefix() + host + ":" + port + "/" + dbName;
    }

    /**
     * Set to use HexWKB and DriverWrapperLW. The JDBC URL will prefix with
     * <code>jdbc:postgresql_lwgis://</code> instead of <code>jdbc:postgresql://</code>.
     */
    public Builder useLW(boolean useLW) {
      this.useLW = useLW;
      if (useLW) {
        // make sure the LW driver is registered
        String driver = checkDriver();
        if (driver != null) {
          log.log(DEBUG, "DriverWrapperLW exists {0}", driver);
        } else {
          log.log(ERROR, "Missing dependency net.postgis:postgis-jdbc for DriverWrapperLW?");
        }
      }
      return this;
    }

    private static String checkDriver() {
      if (classExists("net.postgis.jdbc.DriverWrapperLW")) {
        return "net.postgis.jdbc.DriverWrapperLW";
      }
      if (classExists("org.postgis.DriverWrapperLW")) {
        return "org.postgis.DriverWrapperLW";
      }
      return null;
    }

    private static boolean classExists(String cn) {
      try {
        Class.forName(cn);
        return true;
      } catch (ClassNotFoundException e) {
        return false;
      }
    }

    @Override
    public PostgisContainer build() {
      return new PostgisContainer(this);
    }

    @Override
    public PostgisContainer start() {
      return build().start();
    }
  }
}
