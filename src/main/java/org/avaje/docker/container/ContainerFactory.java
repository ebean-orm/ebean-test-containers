package org.avaje.docker.container;

import org.avaje.docker.commands.MySqlConfig;
import org.avaje.docker.commands.MySqlContainer;
import org.avaje.docker.commands.PostgresConfig;
import org.avaje.docker.commands.PostgresContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Creates containers from properties with the ability to start and stop them.
 */
public class ContainerFactory {

  private final Properties properties;

  private final List<Container> containers = new ArrayList<>();

  /**
   * Create given the properties.
   */
  public ContainerFactory(Properties properties) {
    this.properties = properties;
    init();
  }

  private void init() {
    String pgVersion = version("postgres");
    if (pgVersion != null) {
      containers.add(new PostgresContainer(new PostgresConfig(pgVersion, properties)));
    }
    String mysqlVersion = version("mysql");
    if (mysqlVersion != null) {
      containers.add(new MySqlContainer(new MySqlConfig(mysqlVersion, properties)));
    }
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
   * Stop all containers.
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
        logging.accept(container.config().startDescription());
      }
      container.stop();
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
