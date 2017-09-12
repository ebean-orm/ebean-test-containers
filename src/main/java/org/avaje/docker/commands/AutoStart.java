package org.avaje.docker.commands;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

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
  public void run(Properties properties) {

    DbConfig dbConfig = DbConfigFactory.create(properties);

    if (dbConfig.hasPlatform()) {

      DbCommands db = DbConfigFactory.createCommands(dbConfig);
      log.info(db.getStartDescription());
      db.start();
    }

    // could also start other containers like elasticsearch, redis etc
  }
}
