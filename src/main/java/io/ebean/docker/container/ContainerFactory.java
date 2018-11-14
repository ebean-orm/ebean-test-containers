package io.ebean.docker.container;

import io.ebean.docker.commands.ElasticContainer;
import io.ebean.docker.commands.HanaContainer;
import io.ebean.docker.commands.MySqlContainer;
import io.ebean.docker.commands.OracleContainer;
import io.ebean.docker.commands.PostgresContainer;
import io.ebean.docker.commands.SqlServerContainer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Creates containers from properties with the ability to start and stop them.
 */
public class ContainerFactory {

  private final Properties properties;

  private final List<Container> containers = new ArrayList<>();

  private final Set<String> runWith = new HashSet<>();

  private static String defaultRunWith() {
    String runWith = System.getenv("DOCKER_RUN_WITH");
    return System.getProperty("docker_run_with", runWith);
  }

  /**
   * Create given properties and reading system and env properties for 'run with'.
   */
  public ContainerFactory(Properties properties) {
    this(properties, defaultRunWith());
  }

  /**
   * Create given the properties and runWith.
   *
   * @param properties The properties to configure the containers
   * @param runWith    Comma delimited string with container to run
   */
  public ContainerFactory(Properties properties, String runWith) {
    this.properties = properties;
    initRunWith(runWith);
    init();
  }

  private void initRunWith(String runWithOptions) {
    if (runWithOptions != null) {
      for (String value : runWithOptions.split(",")) {
        runWith.add(value.trim().toLowerCase());
      }
    }
  }

  private void init() {
    String elasticVersion = version("elastic");
    if (elasticVersion != null) {
      containers.add(ElasticContainer.create(elasticVersion, properties));
    }
    String pgVersion = runWithVersion("postgres");
    if (pgVersion != null) {
      containers.add(PostgresContainer.create(pgVersion, properties));
    }
    String mysqlVersion = runWithVersion("mysql");
    if (mysqlVersion != null) {
      containers.add(MySqlContainer.create(mysqlVersion, properties));
    }
    String sqlServerVersion = runWithVersion("sqlserver");
    if (sqlServerVersion != null) {
      containers.add(SqlServerContainer.create(sqlServerVersion, properties));
    }
    String oracleVersion = runWithVersion("oracle");
    if (oracleVersion != null) {
      containers.add(OracleContainer.create(oracleVersion, properties));
    }
    String hanaVersion = runWithVersion("hana");
    if (hanaVersion != null) {
      containers.add(HanaContainer.create(hanaVersion, properties));
    }
  }

  /**
   * Return the version if the container should be added.
   * Filters out database containers using <code>runWith</code>.
   */
  String runWithVersion(String name) {
    String version = version(name);
    if (version == null) {
      return null;
    }
    return (runWith.isEmpty() || runWith.contains(name)) ? version : null;
  }

  private String version(String prefix) {
    return properties.getProperty(prefix + ".version");
  }

  /**
   * Start all the containers.
   */
  public void startContainers() {
    startContainers(null);
  }

  /**
   * Start all the containers with a consumer for logging start descriptions.
   */
  public void startContainers(Consumer<String> logging) {
    for (Container container : containers) {
      if (logging != null) {
        logging.accept(container.config().startDescription());
      }
      container.start();
    }
  }

  /**
   * Stop all containers using the stopMode which defaults to also removing the containers.
   */
  public void stopContainers() {
    stopContainers(null);
  }

  /**
   * Stop all the containers with a consumer for logging stop descriptions.
   */
  public void stopContainers(Consumer<String> logging) {

    for (Container container : containers) {
      if (logging != null) {
        logging.accept(container.config().stopDescription());
      }
      container.stop();
    }
  }

  /**
   * Stop all containers (without removing the containers).
   */
  public void stopOnly() {
    stopOnly(null);
  }

  /**
   * Stop all the containers (without remove) with a consumer for logging stop descriptions.
   */
  public void stopOnly(Consumer<String> logging) {

    for (Container container : containers) {
      if (logging != null) {
        logging.accept(container.config().stopDescription());
      }
      container.stopOnly();
    }
  }

  /**
   * Return the config for a given platform.
   */
  public ContainerConfig config(String platform) {
    Container container = container(platform);
    return (container == null) ? null : container.config();
  }

  /**
   * Return the container for a given platform.
   */
  public Container container(String platform) {
    for (Container container : containers) {
      ContainerConfig config = container.config();
      if (config.platform().equalsIgnoreCase(platform)) {
        return container;
      }
    }
    return null;
  }
}
