package io.ebean.test.containers;

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
      .register(false)
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


  @Override
  public io.ebean.datasource.DataSourceBuilder extraDataSourceBuilder() {
    return io.ebean.datasource.DataSourceBuilder.create()
      .url(dbConfig.jdbcExtraUrl())
      .username(dbConfig.getExtraDbUserWithDefault())
      .password(dbConfig.getExtraDbPasswordWithDefault());
  }

  @Override
  public io.ebean.DatabaseBuilder extraDatabaseBuilder() {
    return io.ebean.Database.builder()
      .dataSourceBuilder(extraDataSourceBuilder())
      .name(dbConfig.getExtraDb())
      .defaultDatabase(false)
      .register(false);
  }

}
