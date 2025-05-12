package io.ebean.test.containers;

interface InternalConfigDb extends InternalConfig {

  String summary();

  boolean isInMemory();

  String getTmpfs();

  String getAdminUsername();

  String getAdminPassword();

  String getDbName();

  String getUsername();

  String getPassword();

  String getSchema();

  String getExtensions();

  String getInitSqlFile();

  String getSeedSqlFile();

  String getExtraDb();

  String getExtraDbUser();

  String getExtraDbUserWithDefault();

  String getExtraDbPassword();

  String getExtraDbPasswordWithDefault();

  String getExtraDbExtensions();

  String getExtraDbInitSqlFile();

  String getExtraDbSeedSqlFile();

  boolean isFastStartMode();

  String getCharacterSet();

  String getCollation();

  boolean isExplicitCollation();

  boolean isDefaultCollation();
}
