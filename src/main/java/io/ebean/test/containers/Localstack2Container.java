package io.ebean.test.containers;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.util.List;
import java.util.Properties;

/**
 * Localstack container that supports both AWS SDK old and new (v1 and v2).
 *
 * <pre>{@code
 *
 *     Localstack2Container container = Localstack2Container.builder("0.14")
 *       // .port(4566)
 *       // .image("localstack/localstack:0.14")
 *       .build();
 *
 *     container.start();
 *
 *     AwsSDKv2 sdk = container.sdk2();
 *     DynamoDBClient amazonDynamoDB = sdk.dynamoDBClient();
 *     createTable(amazonDynamoDB);
 *
 * }</pre>
 */
public class Localstack2Container extends BaseContainer<Localstack2Container> {

  @Override
  public Localstack2Container start() {
    startOrThrow();
    return this;
  }

  /**
   * Create a builder for Localstack2Container given the localstack image version.
   */
  public static Builder builder(String version) {
    return new Builder(version);
  }

  /**
   * Builder for Localstack2Container.
   */
  public static class Builder extends BaseBuilder<Localstack2Container, Builder> {

    private String services = "dynamodb";
    private String awsRegion = "ap-southeast-2";
    private String startWeb;

    /**
     * Create with a version of localstack/localstack (example, 0.14)
     */
    private Builder(String version) {
      super("localstack", 4566, 4566, version);
      this.image = "localstack/localstack:" + version;
    }

    @Override
    protected void extraProperties(Properties properties) {
      super.extraProperties(properties);
      services = prop(properties, "services", services);
      awsRegion = prop(properties, "awsRegion", awsRegion);
      startWeb = prop(properties, "startWeb", startWeb);
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
     * Set the start web option.
     */
    public Builder startWeb(String startWeb) {
      this.startWeb = startWeb;
      return self();
    }

    /**
     * Build and return the LocalstackContainer to then start().
     */
    public Localstack2Container build() {
      return new Localstack2Container(this);
    }

    @Override
    public Localstack2Container start() {
      return build().start();
    }
  }

  private final List<String> serviceNames;
  private final String services;
  private final String awsRegion;
  private final String startWeb;

  /**
   * Create the container using the given config.
   */
  public Localstack2Container(Localstack2Container.Builder builder) {
    super(builder);
    this.services = builder.services;
    this.awsRegion = builder.awsRegion;
    this.startWeb = builder.startWeb;
    this.serviceNames = TrimSplit.split(services);
  }

  private String healthUrl() {
    return String.format("http://%s:%s/health", config.getHost(), config.getPort());
  }

  /**
   * Return the AWS v2 SDK compatible helper that provides API for
   * DynamoDB client, SNSClient, SQQClient etc.
   */
  public AwsSDKv2 sdk() {
    return sdk2();
  }

  /**
   * Return the AWS v2 SDK compatible helper that provides API for
   * DynamoDB client, SNSClient, SQQClient etc.
   */
  public AwsSDKv2 sdk2() {
    return new LocalstackSdkV2(awsRegion, endpoint());
  }

  /**
   * Return the AWS v1 SDK compatible helper that provides API for
   * AmazonDynamoDB client, AmazonSNS, AmazonSQS etc.
   */
  public AwsSDKv1 sdk1() {
    return new LocalstackSdkV1(awsRegion, endpointUrl());
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
      if (serviceEntry.contains(key) && serviceEntry.contains("\"running\"") || serviceEntry.contains("\"available\"")) {
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
    if (notEmpty(services)) {
      args.add("-e");
      args.add("LOCALSTACK_SERVICES=" + services);
    }
    if (notEmpty(awsRegion)) {
      args.add("-e");
      args.add("DEFAULT_REGION=" + awsRegion);
    }
    if (notEmpty(startWeb)) {
      args.add("-e");
      args.add("START_WEB=" + startWeb);
    }
    args.add("-e");
    args.add("DEBUG=1");
    args.add(config.image());
    return createProcessBuilder(args);
  }

}
