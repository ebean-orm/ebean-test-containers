package io.ebean.docker.commands;

import java.util.Properties;

/**
 * Container configuration for amazon/dynamodb-local.
 */
public class LocalDynamoDBConfig extends BaseConfig {

  private String awsRegion = "ap-southeast-2";

  public LocalDynamoDBConfig(String version, Properties properties) {
    super("dynamodb", 8001, 8000, version);
    this.image = "amazon/dynamodb-local:" + version; // ":1.13.2"
    this.checkSkipStop = true;
    this.shutdownMode = StopMode.Remove;
    setProperties(properties);
  }

  @Override
  protected void extraProperties(Properties properties) {
    awsRegion = prop(properties, "awsRegion", awsRegion);
  }

  /**
   * Return the AWS region that will be used. Defaults to ap-southeast-2.
   */
  public String awsRegion() {
    return awsRegion;
  }

  /**
   * Set the AWS region to use.
   */
  public LocalDynamoDBConfig awsRegion(String awsRegion) {
    this.awsRegion = awsRegion;
    return this;
  }

}
