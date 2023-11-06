package io.ebean.test.containers;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Properties;

/**
 * Localstack container support.
 *
 * <pre>{@code
 *
 *     LocalstackContainer container = LocalstackContainer.builder("0.14")
 *       // .port(4566)
 *       // .image("localstack/localstack:0.14")
 *       .build();
 *
 *     container.start();
 *
 *     AmazonDynamoDB amazonDynamoDB = container.dynamoDB();
 *     createTable(amazonDynamoDB);
 *
 * }</pre>
 */
public class LocalstackContainer extends BaseContainer<LocalstackContainer> {

  @Override
  public LocalstackContainer start() {
    startOrThrow();
    return this;
  }

  /**
   * Create a builder for LocalstackContainer given the localstack image version.
   */
  public static Builder builder(String version) {
    return new Builder(version);
  }

  /**
   * Deprecated - migrate to builder().
   */
  @Deprecated
  public static Builder newBuilder(String version) {
    return new Builder(version);
  }

  /**
   * Builder for LocalstackContainer.
   */
  public static class Builder extends BaseConfig<LocalstackContainer, LocalstackContainer.Builder> {

    private String services = "dynamodb";
    private String awsRegion = "ap-southeast-2";
    private String startWeb;// = "0";

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
    public LocalstackContainer build() {
      return new LocalstackContainer(this);
    }

    @Override
    public LocalstackContainer start() {
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
  public LocalstackContainer(LocalstackContainer.Builder builder) {
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
   * Return the AmazonDynamoDB that can be used for this container.
   * <p>
   * This should be used AFTER the container is started.
   */
  public AmazonDynamoDB dynamoDB() {
    return AmazonDynamoDBClientBuilder.standard()
      .withCredentials(credentials())
      .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpointUrl(), awsRegion))
      .build();
  }

  /**
   * Return AmazonKinesis that can be used for this container.
   * <p>
   * This should be used AFTER the container is started.
   */
  public AmazonKinesis kinesis() {
    return AmazonKinesisClientBuilder.standard()
      .withCredentials(credentials())
      .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpointUrl(), awsRegion))
      .build();
  }

  /**
   * Return the AmazonSNS client for this container.
   */
  public AmazonSNS sns() {
    return AmazonSNSClientBuilder.standard()
      .withCredentials(credentials())
      .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpointUrl(), awsRegion))
      .build();
  }

  /**
   * Return the AmazonSQS client for this container.
   */
  public AmazonSQS sqs() {
    return AmazonSQSClientBuilder.standard()
      .withCredentials(credentials())
      .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpointUrl(), awsRegion))
      .build();
  }

  public AWSStaticCredentialsProvider credentials() {
    return new AWSStaticCredentialsProvider(new BasicAWSCredentials("localstack", "localstack"));
  }

  public String endpointUrl() {
    return String.format("http://%s:%s/", config.getHost(), config.getPort());
  }

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
