package io.ebean.docker.commands;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import io.ebean.docker.container.StopMode;

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

  public static class Builder extends DbConfig<LocalDynamoDBContainer, LocalDynamoDBContainer.Builder> {

    private String awsRegion = "ap-southeast-2";

    /**
     * Create with a version of amazon/dynamodb-local (example, 1.13.2)
     */
    public Builder(String version) {
      super("dynamodb", 8001, 8000, version);
      this.image = "amazon/dynamodb-local:" + version; // ":1.13.2"
      this.checkSkipShutdown = true;
      this.shutdownMode = StopMode.Remove;
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

    private String awsRegion() {
      return awsRegion;
    }

    /**
     * Build and return the LocalDynamoContainer to then start().
     */
    public LocalDynamoDBContainer build() {
      return new LocalDynamoDBContainer(this);
    }
  }

  private final Builder localConfig;
  private final String endpointUrl;

  public LocalDynamoDBContainer(Builder builder) {
    super(builder);
    this.localConfig = builder;
    this.endpointUrl = String.format("http://%s:%s", config.getHost(), config.getPort());
  }

  /**
   * Return the Builder given the localstack image version.
   */
  public static Builder newBuilder(String version) {
    return new Builder(version);
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
    args.add(config.image());
    return createProcessBuilder(args);
  }

  private boolean notEmpty(String value) {
    return value != null && !value.isEmpty();
  }

}
