package io.ebean.test.containers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger.Level;
import java.util.Properties;

/**
 * Automatically start containers based on docker run properties file.
 */
public class AutoStart {

  /**
   * Search for docker-run.properties and start containers.
   */
  public static void run() {
    new AutoStart().run(loadProps());
  }

  /**
   * Search for docker-run.properties and stop all the containers.
   */
  public static void stop() {
    new AutoStart().stop(loadProps());
  }

  private static Properties loadProps() {
    Properties properties = new Properties();
    try (InputStream is = AutoStart.class.getResourceAsStream("/docker-run.properties")) {
      if (is != null) {
        properties.load(is);
      }
    } catch (IOException e) {
      Commands.log.log(Level.WARNING, "failed to load docker-run.properties file", e);
    }
    return properties;
  }

  /**
   * Start containers based on the given properties.
   */
  private void run(Properties properties) {
    new ContainerFactory(properties).startContainers();
  }

  /**
   * Start containers based on the given properties.
   */
  private void stop(Properties properties) {
    new ContainerFactory(properties).stopContainers();
  }
}
