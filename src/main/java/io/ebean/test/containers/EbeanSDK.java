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
     * Return a DataSource builder for the extra database (url, username, password).
     */
    io.ebean.datasource.DataSourceBuilder extraDataSourceBuilder();

    /**
     * Return a DataSource builder for the second extra database (url, username, password).
     */
    io.ebean.datasource.DataSourceBuilder extra2DataSourceBuilder();

    /**
     * Return an ebean Database builder for the EXTRA database.
     */
    io.ebean.DatabaseBuilder extraDatabaseBuilder();

    /**
     * Return an ebean Database builder for the second EXTRA2 database.
     */
    io.ebean.DatabaseBuilder extra2DatabaseBuilder();
}
