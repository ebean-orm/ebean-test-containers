package io.ebean.docker.commands;

import io.ebean.docker.container.ContainerBuilder;

import java.io.IOException;
import java.util.List;

/**
 * ElasticSearch container commands.
 */
public class ElasticContainer extends BaseContainer {

  /**
   * Builder for ElasticContainer.
   */
  public static class Builder extends BaseConfig<ElasticContainer,ElasticContainer.Builder> {

    private Builder(String version) {
      super("elastic", 9201, 9200, version);
      this.image = "docker.elastic.co/elasticsearch/elasticsearch:" + version;
    }

    @Override
    public ElasticContainer build() {
      return new ElasticContainer(this);
    }
  }

  private final String healthUrl;

  /**
   * Create new Builder for ElasticContainer.
   */
  public static Builder newBuilder(String version) {
    return new Builder(version);
  }

  private ElasticContainer(Builder builder) {
    super(builder);
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
