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
 */
public class LocalDynamoDBContainer extends BaseContainer {

  private final LocalDynamoDBConfig localConfig;
  private final String endpointUrl;

  public LocalDynamoDBContainer(LocalDynamoDBConfig config) {
    super(config);
    this.localConfig = config;
    this.endpointUrl = String.format("http://%s:%s", config.getHost(), config.getPort());
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
