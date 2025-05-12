package io.ebean.test.containers;

import java.util.Properties;

final class ExtraAttributes implements ExtraBuilder {

  String dbName;
  String extensions;
  String username;
  String password = "test";
  String initSqlFile;
  String seedSqlFile;

  String dbName() {
    return dbName;
  }

  String userWithDefaults(String username) {
    String un = userWithDefault();
    return un != null && !un.equals(username) ? un : null;
  }

  private String userWithDefault() {
    return username != null ? username : dbName;
  }

  String passwordWithDefault(String password) {
    return this.password != null ? this.password : password;
  }

  String extensions() {
    return extensions;
  }

  String initSqlFile() {
    return initSqlFile;
  }

  String seedSqlFile() {
    return seedSqlFile;
  }

  void load(String platform, String prefix, Properties properties) {
    dbName = prop(platform, properties, prefix + ".dbName", prop(platform, properties, prefix, dbName));
    username = prop(platform, properties, prefix + ".username", username);
    password = prop(platform, properties, prefix + ".password", password);
    extensions = prop(platform, properties, prefix + ".extensions", extensions);
    initSqlFile = prop(platform, properties, prefix + ".initSqlFile", initSqlFile);
    seedSqlFile = prop(platform, properties, prefix + ".seedSqlFile", seedSqlFile);
  }

  private String prop(String platform, Properties properties, String key, String defaultValue) {
    String val = properties.getProperty("ebean.test." + platform + "." + key, defaultValue);
    return properties.getProperty(platform + "." + key, val);
  }

  @Override
  public ExtraBuilder dbName(String dbName) {
    this.dbName = dbName;
    return this;
  }

  @Override
  public ExtraBuilder extensions(String extensions) {
    this.extensions = extensions;
    return this;
  }

  @Override
  public ExtraBuilder username(String username) {
    this.username = username;
    return this;
  }

  @Override
  public ExtraBuilder password(String password) {
    this.password = password;
    return this;
  }

  @Override
  public ExtraBuilder initSqlFile(String initSqlFile) {
    this.initSqlFile = initSqlFile;
    return this;
  }

  @Override
  public ExtraBuilder seedSqlFile(String seedSqlFile) {
    this.seedSqlFile = seedSqlFile;
    return this;
  }
}
