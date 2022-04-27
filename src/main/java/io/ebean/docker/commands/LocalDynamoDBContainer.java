package io.ebean.docker.commands;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;

import java.util.List;
import java.util.Properties;

/**
 * Container using amazon/dynamodb-local.
 *
 * <pre>{@code
 *
 *     LocalDynamoDBContainer container = LocalDynamoDBContainer.newBuilder("1.13.2")
 *       //.port(8001)
 *       //.containerName("ut_dynamodb")
 *       //.image("amazon/dynamodb-local:1.13.2")
 *       .build();
 *
 *     // start the container (if not already started)
 *     container.start();
 *
 *     // obtain the AWS DynamoDB client
 *     AmazonDynamoDB amazonDynamoDB = container.dynamoDB();
 *
 *     createTableIfNeeded(amazonDynamoDB);
 *
 *     // container will be shutdown and removed via shutdown hook
 *     // local devs touch ~/.ebean/ignore-docker-shutdown
 *     // to keep the container running for faster testing etc
 *
 * }</pre>
 *
 * <h3>Shutdown</h3>
 * <p>
 * By default, the container will be shutdown and removed via shutdown hook
 *
 * <h3>Local development</h3>
 * <p>
 * For local development we typically want to keep the container running such that
 * tests are fast to run. To do this:
 *
 * <pre>
 *   touch ~/.ebean/ignore-docker-shutdown
 * </pre>
 */
public class LocalDynamoDBContainer extends BaseContainer {


  public static class Builder {

    protected final String version;
    protected final Properties properties;
    protected LocalDynamoDBConfig config;

    /**
     * Create with a version of amazon/dynamodb-local (example, 1.13.2)
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
     * Explicitly set the image to use. Defaults to amazon/dynamodb-local:version.
     */
    public Builder image(String image) {
      config().setImage(image);
      return this;
    }

    /**
     * Set the exposed port. Defaults to 8001.
     */
    public Builder port(int port) {
      config().setPort(port);
      return this;
    }

    /**
     * Set the container name. Defaults to ut_dynamodb.
     */
    public Builder containerName(String containerName) {
      config().setContainerName(containerName);
      return this;
    }

    /**
     * Return the underlying configuration.
     */
    public LocalDynamoDBConfig config() {
      if (config == null) {
        config = new LocalDynamoDBConfig(version, properties);
      }
      return config;
    }

    /**
     * Build and return the LocalDynamoContainer to then start().
     */
    public LocalDynamoDBContainer build() {
      return new LocalDynamoDBContainer(config());
    }
  }


  private final LocalDynamoDBConfig localConfig;
  private final String endpointUrl;

  public LocalDynamoDBContainer(LocalDynamoDBConfig config) {
    super(config);
    this.localConfig = config;
    this.endpointUrl = String.format("http://%s:%s", config.getHost(), config.getPort());
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
   * Create the LocalDynamoContainer with configuration via properties.
   */
  public static LocalDynamoDBContainer create(String version, Properties properties) {
    return new LocalDynamoDBContainer(new LocalDynamoDBConfig(version, properties));
  }

  /**
   * Return the AmazonDynamoDB that can be used for this container.
   * <p>
   * This should be used AFTER the container is started.
   */
  public AmazonDynamoDB dynamoDB() {
    return AmazonDynamoDBClientBuilder.standard()
      .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("localstack", "localstack")))
      .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpointUrl, localConfig.awsRegion()))
      .build();
  }

  @Override
  public boolean checkConnectivity() {
    // appears to be immediately available
    return true;
  }

  protected ProcessBuilder runProcess() {
    List<String> args = dockerRun();
    if (notEmpty(localConfig.awsRegion())) {
      args.add("-e");
      args.add("DEFAULT_REGION=" + localConfig.awsRegion());
    }
    args.add("-e");
    args.add("AWS_ACCESS_KEY_ID=localstack");
    args.add("-e");
    args.add("AWS_SECRET_KEY=localstack");
    args.add(config.image);
    return createProcessBuilder(args);
  }

  private boolean notEmpty(String value) {
    return value != null && !value.isEmpty();
  }

}
