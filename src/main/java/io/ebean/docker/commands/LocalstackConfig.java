package io.ebean.docker.commands;

import java.util.Properties;

/**
 * Configuration for Localstack container.
 */
public class LocalstackConfig extends BaseConfig {

  private String services = "dynamodb";
  private String awsRegion = "ap-southeast-2";
  private String startWeb = "0";

  /**
   * Create with image version and properties.
   */
  public LocalstackConfig(String version, Properties properties) {
    super("localstack", 4566, 4566, version);
    setAdminPort(4571);
    setAdminInternalPort(4571);
    this.checkSkipStop = true;
    this.shutdownMode = StopMode.Remove;
    this.image = "localstack/localstack:" + version;
    setProperties(properties);
  }

  @Override
  protected void extraProperties(Properties properties) {
    super.extraProperties(properties);
    services = prop(properties, "services", services);
    awsRegion = prop(properties, "awsRegion", awsRegion);
    startWeb = prop(properties, "startWeb", startWeb);
  }

  /**
   * Return the services desired (comma delimited).
   */
  public String services() {
    return services;
  }

  /**
   * Set the services desired (comma delimited). Defaults to dynamodb.
   */
  public LocalstackConfig services(String services) {
    this.services = services;
    return this;
  }

  /**
   * Return the AWS region to use. Defaults to ap-southeast-2.
   */
  public String awsRegion() {
    return awsRegion;
  }

  /**
   * Set the AWS region to use.
   */
  public LocalstackConfig awsRegion(String awsRegion) {
    this.awsRegion = awsRegion;
    return this;
  }

  /**
   * Return the start web option.
   */
  public String startWeb() {
    return startWeb;
  }

  /**
   * Set the start web option.
   */
  public LocalstackConfig startWeb(String startWeb) {
    this.startWeb = startWeb;
    return this;
  }
}
