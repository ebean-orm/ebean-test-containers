package io.ebean.test.containers;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import java.util.List;
import java.util.Properties;

/**
 * MongoDB container.
 *
 * <pre>{@code
 *
 *   MongoContainer container = MongoContainer.builder("8.0")
 *     //.port(27017)
 *     //.containerName("ut_mongodb")
 *     //.username("test")
 *     //.password("test")
 *     //.dbName("test")
 *     .build();
 *
 *   container.start();
 *
 *   // obtain connection string
 *   String connectionString = container.connectionString();
 *
 *   // obtain a MongoClient (requires mongodb-driver-sync on the classpath)
 *   MongoClient client = container.mongoClient();
 *   MongoDatabase db = client.getDatabase(container.dbName());
 *
 *   // container will be shutdown and removed via shutdown hook
 *   // local devs: touch ~/.ebean/ignore-docker-shutdown
 *   // to keep the container running for faster testing
 *
 * }</pre>
 *
 * <h3>Shutdown</h3>
 * <p>
 * By default, the container will be stopped and removed via shutdown hook.
 *
 * <h3>Local development</h3>
 * <p>
 * For local development, keep the container running between test runs:
 *
 * <pre>
 *   touch ~/.ebean/ignore-docker-shutdown
 * </pre>
 */
public class MongoContainer extends BaseContainer<MongoContainer> {

  @Override
  public MongoContainer start() {
    startOrThrow();
    return this;
  }

  /**
   * Create a builder for MongoContainer.
   */
  public static Builder builder(String version) {
    return new Builder(version);
  }

  /**
   * The MongoContainer builder.
   */
  public static class Builder extends BaseBuilder<MongoContainer, Builder> {

    private String username = "test";
    private String password = "test";
    private String dbName = "test";

    private Builder(String version) {
      super("mongo", 27017, 27017, version);
    }

    /**
     * Set the MongoDB admin username. Defaults to "test".
     */
    public Builder username(String username) {
      this.username = username;
      return this;
    }

    /**
     * Set the MongoDB admin password. Defaults to "test".
     */
    public Builder password(String password) {
      this.password = password;
      return this;
    }

    /**
     * Set the initial database name. Defaults to "test".
     */
    public Builder dbName(String dbName) {
      this.dbName = dbName;
      return this;
    }

    @Override
    protected void extraProperties(Properties properties) {
      username = prop(properties, "username", username);
      password = prop(properties, "password", password);
      dbName = prop(properties, "dbName", dbName);
    }

    @Override
    public MongoContainer build() {
      return new MongoContainer(this);
    }

    @Override
    public MongoContainer start() {
      return build().start();
    }
  }

  private final String username;
  private final String password;
  private final String dbName;

  private MongoContainer(Builder builder) {
    super(builder);
    this.username = builder.username;
    this.password = builder.password;
    this.dbName = builder.dbName;
  }

  /**
   * Return the database name.
   */
  public String dbName() {
    return dbName;
  }

  /**
   * Return the MongoDB connection string for this container.
   */
  public String connectionString() {
    if (notEmpty(username) && notEmpty(password)) {
      // MONGO_INITDB_ROOT_USERNAME/PASSWORD always create the root user in the "admin" database
      return String.format("mongodb://%s:%s@%s:%d/%s?authSource=admin", username, password, config.getHost(), config.getPort(), dbName);
    }
    return String.format("mongodb://%s:%d/%s", config.getHost(), config.getPort(), dbName);
  }

  /**
   * Return a MongoClient connected to this container.
   * <p>
   * Requires {@code mongodb-driver-sync} on the classpath.
   */
  public MongoClient mongoClient() {
    return MongoClients.create(connectionString());
  }

  @Override
  boolean checkConnectivity() {
    // clearMatch discards the first "Waiting for connections" that appears during
    // the credential-setup phase (binds to 127.0.0.1 only) so we wait for the
    // second occurrence that binds to 0.0.0.0 and is actually reachable.
    // When no auth is configured the clearMatch never triggers, but the single
    // "Waiting for connections" still matches — so this is correct in both cases.
    return logsContain("Waiting for connections", "MongoDB init process complete");
  }

  @Override
  protected ProcessBuilder runProcess() {
    List<String> args = dockerRun();
    if (notEmpty(username)) {
      args.add("-e");
      args.add("MONGO_INITDB_ROOT_USERNAME=" + username);
    }
    if (notEmpty(password)) {
      args.add("-e");
      args.add("MONGO_INITDB_ROOT_PASSWORD=" + password);
    }
    if (notEmpty(dbName)) {
      args.add("-e");
      args.add("MONGO_INITDB_DATABASE=" + dbName);
    }
    args.add(config.image());
    return createProcessBuilder(args);
  }
}
