package io.ebean.test.containers;

/**
 * Ebean SDK extension to create builders for Ebean Database and DataSource.
 * <pre>{@code
 *
 *  PostgresContainer container = PostgresContainer.builder("15")
 *    .dbName("my_test")
 *    .build()
 *    .start();
 *
 *  io.ebean.Database ebean = container.ebean().builder().build();
 *  // create ebean database and use it
 *
 * }</pre>
 */
public interface EbeanSDK {

    /**
     * Return an ebean Database builder for the underlying database (url, username, password).
     * <p>
     * This defaults to register with Ebean's io.ebean.DB as the default database.
     * The name of the ebean database will be dbName set for the container.
     * <p>
     * This builder will have ddlGenerate set to true and ddlRun set to true. Alternatively,
     * set runMigrations(true) to run database migrations on startup.
     */
    io.ebean.DatabaseBuilder builder();

    /**
     * Return a DataSource builder for the underlying database (url, username, password).
     */
    io.ebean.datasource.DataSourceBuilder dataSourceBuilder();

    /**
     * Return an ebean Database builder for the EXTRA database.
     * <p>
     * This is set to not register with Ebean's io.ebean.DB.
     */
    io.ebean.DatabaseBuilder extraDatabaseBuilder();

    /**
     * Return a DataSource builder for the extra database (url, username, password).
     */
    io.ebean.datasource.DataSourceBuilder extraDataSourceBuilder();

    /**
     * Return an ebean Database builder for the second EXTRA2 database.
     * <p>
     * This is set to not register with Ebean's io.ebean.DB.
     */
    io.ebean.DatabaseBuilder extra2DatabaseBuilder();

    /**
     * Return a DataSource builder for the second extra database (url, username, password).
     */
    io.ebean.datasource.DataSourceBuilder extra2DataSourceBuilder();

}
