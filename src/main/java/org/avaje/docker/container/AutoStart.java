package org.avaje.docker.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Automatically start containers based on docker run properties file.
 */
public class AutoStart {

  private static Logger log = LoggerFactory.getLogger(AutoStart.class);

  /**
   * Search for docker-run.properties and start containers.
   */
  public static void run() {
    try (InputStream is = AutoStart.class.getResourceAsStream("/docker-run.properties")) {
      if (is != null) {
        Properties properties = new Properties();
        properties.load(is);
        new AutoStart().run(properties);
      }
    } catch (IOException e) {
      log.warn("failed to load docker-run.properties file", e);
    }
  }

  /**
   * Start containers based on the given properties.
   */
  private void run(Properties properties) {
    new ContainerFactory(properties).startContainers();
  }
}
