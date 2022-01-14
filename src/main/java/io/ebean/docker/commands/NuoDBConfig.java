package io.ebean.docker.commands;

import java.util.Properties;

public class NuoDBConfig extends DbConfig {

  private String network = "nuodb-net";
  private String sm1 = "sm";
  private String te1 = "te";
  private String labels = "node localhost";

  private int port2 = 48004;
  private int internalPort2 = 48004;
  private int port3 = 48005;
  private int internalPort3 = 48005;

  public NuoDBConfig(String version, Properties properties) {
    this(version);
    setProperties(properties);
  }

  public NuoDBConfig(String version) {
    super("nuodb", 8888, 8888, version);
    this.containerName = platform;
    this.image = "nuodb/nuodb-ce:" + version;
    this.adminUsername = "dba";
    this.adminPassword = "dba";
    // for testing purposes generally going to use single 'testdb'
    // and different apps have different schema
    this.dbName = "testdb";
  }

  public NuoDBConfig() {
    this("4.0");
  }

  @Override
  public String summary() {
    return "host:" + host + " port:" + port + " db:" + dbName + " schema:" + schema + " user:" + username + "/" + password;
  }

  public String jdbcUrl() {
    return "jdbc:com.nuodb://" + getHost() + "/" + getDbName();
  }

  public int getPort2() {
    return port2;
  }

  public void setPort2(int port2) {
    this.port2 = port2;
  }

  public int getInternalPort2() {
    return internalPort2;
  }

  public void setInternalPort2(int internalPort2) {
    this.internalPort2 = internalPort2;
  }

  public int getPort3() {
    return port3;
  }

  public void setPort3(int port3) {
    this.port3 = port3;
  }

  public int getInternalPort3() {
    return internalPort3;
  }

  public void setInternalPort3(int internalPort3) {
    this.internalPort3 = internalPort3;
  }

  public String getNetwork() {
    return network;
  }

  public NuoDBConfig setNetwork(String network) {
    this.network = network;
    return this;
  }

  public String getSm1() {
    return sm1;
  }

  public NuoDBConfig setSm1(String sm1) {
    this.sm1 = sm1;
    return this;
  }

  public String getTe1() {
    return te1;
  }

  public NuoDBConfig setTe1(String te1) {
    this.te1 = te1;
    return this;
  }

  public String getLabels() {
    return labels;
  }

  public NuoDBConfig setLabels(String labels) {
    this.labels = labels;
    return this;
  }

}
