package io.ebean.test.containers;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.util.List;
import java.util.Properties;

/**
 * Floci container that supports AWS SDK v2.
 *
 * <pre>{@code
 *
 *     FlociContainer container = FlociContainer.builder("latest")
 *       // .port(4566)
 *       // .image("hectorvent/floci:latest")
 *       .build();
 *
 *     container.start();
 *
 *     AwsSDKv2 sdk = container.sdk2();
 *     var amazonDynamoDB = sdk.dynamoDBClient();
 *     createTable(amazonDynamoDB);
 *
 * }</pre>
 */
public class FlociContainer extends BaseContainer<FlociContainer> {

  @Override
  public FlociContainer start() {
    startOrThrow();
    return this;
  }

  /**
   * Create a builder for FlociContainer given the Floci image version.
   */
  public static Builder builder(String version) {
    return new Builder(version);
  }

  /**
   * Builder for FlociContainer.
   */
  public static class Builder extends BaseBuilder<FlociContainer, Builder> {

    private String services = "dynamodb";
    private String awsRegion = "ap-southeast-2";
    private String healthUri = "_floci/health";

    /**
     * Create with a version of hectorvent/floci (example, latest)
     */
    private Builder(String version) {
      super("floci", 4566, 4566, version);
      this.image = "hectorvent/floci:" + version;
    }

    @Override
    protected void extraProperties(Properties properties) {
      super.extraProperties(properties);
      services = prop(properties, "services", services);
      awsRegion = prop(properties, "awsRegion", awsRegion);
      healthUri = prop(properties, "healthUri", healthUri);
    }

    /**
     * Set the services desired (comma delimited). Defaults to "dynamodb".
     * <p>
     * Examples: "dynamodb", "dynamodb,sns,sqs,kinesis"
     */
    public Builder services(String services) {
      this.services = services;
      return self();
    }

    /**
     * Set the AWS region to use. For example, "ap-southeast-2".
     */
    public Builder awsRegion(String awsRegion) {
      this.awsRegion = awsRegion;
      return self();
    }

    /**
     * Set the healthUri option - defaults to _floci/health.
     */
    public Builder healthUri(String healthUri) {
      this.healthUri = healthUri;
      return self();
    }

    /**
     * Build and return the FlociContainer to then start().
     */
    public FlociContainer build() {
      return new FlociContainer(this);
    }

    @Override
    public FlociContainer start() {
      return build().start();
    }
  }

  private final List<String> serviceNames;
  private final String awsRegion;
  private final String healthUri;

  /**
   * Create the container using the given config.
   */
  public FlociContainer(Builder builder) {
    super(builder);
    this.awsRegion = builder.awsRegion;
    this.healthUri = builder.healthUri;
    this.serviceNames = TrimSplit.split(builder.services);
  }

  private String healthUrl() {
    return String.format("http://%s:%s/%s", config.getHost(), config.getPort(), healthUri);
  }

  /**
   * Return the AWS v2 SDK compatible helper that provides API for
   * DynamoDB client, SnsClient, SqsClient etc.
   */
  public AwsSDKv2 sdk() {
    return sdk2();
  }

  /**
   * Return the AWS v2 SDK compatible helper that provides API for
   * DynamoDB client, SnsClient, SqsClient etc.
   */
  public AwsSDKv2 sdk2() {
    return new LocalstackSdkV2(awsRegion, endpoint());
  }

  /**
   * Return the endpoint as URI.
   */
  public URI endpoint() {
    return URI.create(endpointUrl());
  }

  /**
   * Return the endpoint as String.
   */
  public String endpointUrl() {
    return String.format("http://%s:%s/", config.getHost(), config.getPort());
  }

  /**
   * Return the AWS region.
   */
  public String awsRegion() {
    return awsRegion;
  }

  @Override
  boolean checkConnectivity() {
    try {
      String content = readUrlContent(healthUrl());
      if (log.isLoggable(Level.TRACE)) {
        log.log(Level.TRACE, "checkConnectivity content: {0}", content);
      }
      return checkStatus(content);
    } catch (IOException e) {
      return false;
    }
  }

  private boolean checkStatus(String content) {
    String[] serviceEntries = content.split(",");
    for (String serviceName : serviceNames) {
      if (!isServiceReady(serviceName, serviceEntries)) {
        return false;
      }
    }
    return true;
  }

  private boolean isServiceReady(String serviceName, String[] serviceEntries) {
    String key = "\"" + serviceName + "\":";
    for (String serviceEntry : serviceEntries) {
      if (serviceEntry.contains(key) && (serviceEntry.contains("\"running\"") || serviceEntry.contains("\"available\""))) {
        return true;
      }
    }
    return false;
  }

  protected ProcessBuilder runProcess() {
    List<String> args = dockerRun();
    if (config.getAdminPort() > 0) {
      args.add("-p");
      args.add(config.getAdminPort() + ":" + config.getAdminInternalPort());
    }
    if (notEmpty(awsRegion)) {
      args.add("-e");
      args.add("FLOCI_DEFAULT_REGION=" + awsRegion);
    }
    args.add(config.image());
    return createProcessBuilder(args);
  }
}
