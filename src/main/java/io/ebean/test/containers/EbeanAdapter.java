package io.ebean.test.containers;

import io.ebean.DatabaseBuilder;
import io.ebean.datasource.DataSourceBuilder;

final class EbeanAdapter implements EbeanSDK {

    private final InternalConfigDb dbConfig;

    EbeanAdapter(InternalConfigDb dbConfig) {
        this.dbConfig = dbConfig;
    }

    @Override
    public io.ebean.DatabaseBuilder builder() {
        return io.ebean.Database.builder()
                .dataSourceBuilder(dataSourceBuilder())
                .name(dbConfig.getDbName())
                .ddlGenerate(true)
                .ddlRun(true);
    }

    @Override
    public io.ebean.datasource.DataSourceBuilder dataSourceBuilder() {
        return io.ebean.datasource.DataSourceBuilder.create()
                .url(dbConfig.jdbcUrl())
                .username(dbConfig.getUsername())
                .password(dbConfig.getPassword());
    }

    private DataSourceBuilder dataSourceBuilder(ExtraAttributes extra) {
        final String username = extra.userWithDefaults(dbConfig.getUsername());
        final String password = extra.passwordWithDefault(dbConfig.getPassword());
        return DataSourceBuilder.create()
                .url(dbConfig.jdbcUrl(extra.dbName()))
                .username(username)
                .password(password);
    }

    @Override
    public io.ebean.datasource.DataSourceBuilder extraDataSourceBuilder() {
        return dataSourceBuilder(dbConfig.extra());
    }

    @Override
    public io.ebean.datasource.DataSourceBuilder extra2DataSourceBuilder() {
        return dataSourceBuilder(dbConfig.extra2());
    }

    @Override
    public io.ebean.DatabaseBuilder extraDatabaseBuilder() {
        return extraBuilder(extraDataSourceBuilder(), dbConfig.extra().dbName());
    }

    @Override
    public io.ebean.DatabaseBuilder extra2DatabaseBuilder() {
        return extraBuilder(extra2DataSourceBuilder(), dbConfig.extra2().dbName());
    }

    private static DatabaseBuilder extraBuilder(DataSourceBuilder ds, String name) {
        return io.ebean.Database.builder()
                .dataSourceBuilder(ds)
                .name(name)
                .defaultDatabase(false)
                .register(false);
    }

}
