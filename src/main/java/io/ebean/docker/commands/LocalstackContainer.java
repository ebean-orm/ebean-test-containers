package io.ebean.docker.commands;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * Localstack container support.
 *
 * <pre>{@code
 *
 *     LocalstackContainer container = LocalstackContainer.newBuilder("0.14")
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
public class LocalstackContainer extends BaseContainer {

  /**
   * Builder for LocalstackContainer.
   */
  public static class Builder {

    protected final String version;
    protected final Properties properties;
    protected LocalstackConfig config;

    /**
     * Create with a version of localstack/localstack (example, 0.14)
     */
    public Builder(String version) {
      this.version = version;
      this.properties = null;
    }

    /**
     * Create with a version and properties.
     */
    public Builder(String version, Properties properties) {
      this.version = version;
      this.properties = properties;
    }

    /**
     * Set the services desired in comma delimited form.
     */
    public Builder services(String services) {
      config().services(services);
      return this;
    }

    /**
     * Explicitly set the image to use. Defaults to localstack/localstack:version.
     */
    public Builder image(String image) {
      config().setImage(image);
      return this;
    }

    /**
     * Set the exposed port. Defaults to 4566.
     */
    public Builder port(int port) {
      config().setPort(port);
      return this;
    }

    /**
     * Set the container name. Defaults to ut_localstack.
     */
    public Builder containerName(String containerName) {
      config().setContainerName(containerName);
      return this;
    }

    /**
     * Return the underlying configuration.
     */
    public LocalstackConfig config() {
      if (config == null) {
        config = new LocalstackConfig(version, properties);
      }
      return config;
    }

    /**
     * Build and return the LocalstackContainer to then start().
     */
    public LocalstackContainer build() {
//      System.setProperty("aws.region", config().awsRegion());
//      System.setProperty("com.amazonaws.sdk.disableCbor", "true");
//      System.setProperty("aws.accesskey", "localstack");
//      System.setProperty("aws.secretkey", "localstack");

//      System.setProperty("aws.lambda.function.name", "consolidation-test");
//      System.setProperty("stream.name", randomAlphabetic(10));
//      System.setProperty("stack.name", "consolidation-test");

      return new LocalstackContainer(config());
    }
  }

  private final LocalstackConfig localConfig;
  private final List<String> serviceNames;
  private final String healthUrl;
  private final String endpointUrl;

  /**
   * Create the container using the given config.
   */
  public LocalstackContainer(LocalstackConfig config) {
    super(config);
    this.localConfig = config;
    this.serviceNames = TrimSplit.split(config.services());
    this.healthUrl = String.format("http://%s:%s/health", config.getHost(), config.getPort());
    this.endpointUrl = String.format("http://%s:%s/", config.getHost(), config.getPort());
  }

  /**
   * Return the Builder given the localstack image version.
   */
  public static Builder newBuilder(String version) {
    return new Builder(version);
  }

  /**
   * Return the Builder given the localstack image version and properties.
   */
  public static Builder newBuilder(String version, Properties properties) {
    return new Builder(version, properties);
  }

  /**
   * Create the ElasticContainer with configuration via properties.
   */
  public static LocalstackContainer create(String version, Properties properties) {
    return new LocalstackContainer(new LocalstackConfig(version, properties));
  }

  /**
   * Return the AmazonDynamoDB that can be used for this container.
   * <p>
   * This should be used AFTER the container is started.
   */
  public AmazonDynamoDB dynamoDB() {
    return AmazonDynamoDBClientBuilder.standard()
      .withCredentials(credentials())
      .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpointUrl, localConfig.awsRegion()))
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
      .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpointUrl, localConfig.awsRegion()))
      .build();
  }

  public AmazonSNS sns() {
    return AmazonSNSClientBuilder.standard()
      .withCredentials(credentials())
      .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpointUrl, localConfig.awsRegion()))
      .build();
  }

  public AWSStaticCredentialsProvider credentials() {
    return new AWSStaticCredentialsProvider(new BasicAWSCredentials("localstack", "localstack"));
  }

  public String endpointUrl() {
    return endpointUrl;
  }

  public String awsRegion() {
    return localConfig.awsRegion();
  }

  @Override
  boolean checkConnectivity() {
    try {
      String content = readUrlContent(healthUrl);
      if (log.isTraceEnabled()) {
        log.trace("checkConnectivity content: {}", content);
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
    args.add("-p");
    args.add(config.getAdminPort() + ":" + config.getAdminInternalPort());

    if (notEmpty(localConfig.services())) {
      args.add("-e");
      args.add("LOCALSTACK_SERVICES=" + localConfig.services());
    }
    if (notEmpty(localConfig.awsRegion())) {
      args.add("-e");
      args.add("DEFAULT_REGION=" + localConfig.awsRegion());
    }
    if (notEmpty(localConfig.startWeb())) {
      args.add("-e");
      args.add("START_WEB=" + localConfig.startWeb());
    }
    args.add(config.image);
    return createProcessBuilder(args);
  }

  private boolean notEmpty(String value) {
    return value != null && !value.isEmpty();
  }

}
