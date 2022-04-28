package io.ebean.docker.commands;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class ElasticContainer extends BaseContainer {

  private final String healthUrl;

  /**
   * Create the ElasticContainer with configuration via properties.
   */
  public static ElasticContainer create(String elasticVersion, Properties properties) {
    return new ElasticContainer(new ElasticConfig(elasticVersion, properties));
  }

  public ElasticContainer(ElasticConfig config) {
    super(config);
    this.healthUrl = String.format("http://%s:%s/", config.getHost(), config.getPort());
  }

  @Override
  boolean checkConnectivity() {
    try {
      return readUrlContent(healthUrl).contains("docker-cluster");
    } catch (IOException e) {
      return false;
    }
  }

  protected ProcessBuilder runProcess() {
    List<String> args = dockerRun();
    args.add("-e");
    args.add("http.host=0.0.0.0");
    args.add("-e");
    args.add("transport.host=127.0.0.1");
    args.add("-e");
    args.add("xpack.security.enabled=false");
    args.add(config.image());
    return createProcessBuilder(args);
  }

}
