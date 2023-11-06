package io.ebean.test.containers;

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
 *     LocalDynamoDBContainer container = LocalDynamoDBContainer.builder"1.13.2")
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
public class LocalDynamoDBContainer extends BaseContainer<LocalDynamoDBContainer> {

  @Override
  public LocalDynamoDBContainer start() {
    startOrThrow();
    return this;
  }

  /**
   * Create a builder for LocalDynamoDBContainer given the localstack image version.
   */
  public static Builder builder(String version) {
    return new Builder(version);
  }

  public static class Builder extends BaseConfig<LocalDynamoDBContainer, LocalDynamoDBContainer.Builder> {

    private String awsRegion = "ap-southeast-2";

    /**
     * Create with a version of amazon/dynamodb-local (example, 1.13.2)
     */
    private Builder(String version) {
      super("dynamodb", 8001, 8000, version);
      this.image = "amazon/dynamodb-local:" + version; // ":1.13.2"
    }

    @Override
    protected void extraProperties(Properties properties) {
      awsRegion = prop(properties, "awsRegion", awsRegion);
    }

    /**
     * Set the AWS region to use.
     */
    public Builder awsRegion(String awsRegion) {
      this.awsRegion = awsRegion;
      return this;
    }

    /**
     * Build and return the LocalDynamoContainer to then start().
     */
    public LocalDynamoDBContainer build() {
      return new LocalDynamoDBContainer(this);
    }

    @Override
    public LocalDynamoDBContainer start() {
      return build().start();
    }
  }

  private final String awsRegion;

  public LocalDynamoDBContainer(Builder builder) {
    super(builder);
    this.awsRegion = builder.awsRegion;
  }

  /**
   * Return the endpoint URL that can be used to connect to this container.
   */
  public String endpointUrl() {
    return String.format("http://%s:%s", config.getHost(), config.getPort());
  }

  /**
   * Return the AmazonDynamoDB that can be used for this container.
   * <p>
   * This should be used AFTER the container is started.
   */
  public AmazonDynamoDB dynamoDB() {
    return AmazonDynamoDBClientBuilder.standard()
      .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("localstack", "localstack")))
      .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpointUrl(), awsRegion))
      .build();
  }

  @Override
  public boolean checkConnectivity() {
    // appears to be immediately available
    return true;
  }

  protected ProcessBuilder runProcess() {
    List<String> args = dockerRun();
    if (notEmpty(awsRegion)) {
      args.add("-e");
      args.add("DEFAULT_REGION=" + awsRegion);
    }
    args.add("-e");
    args.add("AWS_ACCESS_KEY_ID=localstack");
    args.add("-e");
    args.add("AWS_SECRET_KEY=localstack");
    args.add(config.image());
    return createProcessBuilder(args);
  }

}
