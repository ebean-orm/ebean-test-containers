package io.ebean.docker.commands;

import java.util.Properties;

/**
 * Configure and create local DynamoDB container.
 *
 * <pre>{@code
 *
 *     LocalDynamoDBContainer container = new LocalDynamoDB("1.13.2")
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
public class LocalDynamoDB {

  protected final String version;
  protected final Properties properties;
  protected LocalDynamoDBConfig config;

  /**
   * Create with a version of amazon/dynamodb-local (example, 1.13.2)
   */
  public LocalDynamoDB(String version) {
    this.version = version;
    this.properties = null;
  }

  /**
   * Create with a version and properties.
   */
  public LocalDynamoDB(String version, Properties properties) {
    this.version = version;
    this.properties = properties;
  }

  /**
   * Explicitly set the image to use. Defaults to amazon/dynamodb-local:version.
   */
  public LocalDynamoDB image(String image) {
    config().setImage(image);
    return this;
  }

  /**
   * Set the exposed port. Defaults to 8001.
   */
  public LocalDynamoDB port(int port) {
    config().setPort(port);
    return this;
  }

  /**
   * Set the container name. Defaults to ut_dynamodb.
   */
  public LocalDynamoDB containerName(String containerName) {
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
