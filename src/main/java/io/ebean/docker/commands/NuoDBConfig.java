package io.ebean.docker.commands;

import java.util.Properties;

public class NuoDBConfig extends DbConfig {

  private String network = "nuodb-net";
  private String sm1 = "sm1";
  private String te1 = "te1";
  private String labels = "node localhost";

  private int port2 = 48005;
  private int internalPort2 = 48005;

  public NuoDBConfig(String version, Properties properties) {
    this(version);
    setProperties(properties);
  }

  public NuoDBConfig(String version) {
    super("nuodb", "48004", "48004", version);
    this.image = "nuodb/nuodb-ce:" + version;
    this.adminInternalPort = "8888";
    this.adminPort = "8888";
    this.adminUsername = "dba";
    this.adminPassword = "dba";
  }

  public NuoDBConfig() {
    this("4.0.0");
  }

  public String jdbcUrl() {
    return "jdbc:com.nuodb://localhost/" + getDbName();
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
