package io.ebean.test.containers;

import java.io.IOException;
import java.util.List;

/**
 * ElasticSearch container commands.
 */
public class ElasticContainer extends BaseContainer {

  /**
   * Create a builder for ElasticContainer.
   */
  public static Builder builder(String version) {
    return new Builder(version);
  }

  /**
   * Deprecated - migrate to builder().
   */
  @Deprecated
  public static Builder newBuilder(String version) {
    return builder(version);
  }

  /**
   * Builder for ElasticContainer.
   */
  public static class Builder extends BaseConfig<ElasticContainer, ElasticContainer.Builder> {

    private Builder(String version) {
      super("elastic", 9201, 9200, version);
      this.image = "docker.elastic.co/elasticsearch/elasticsearch:" + version;
      this.maxReadyAttempts = 400;
    }

    @Override
    public ElasticContainer build() {
      return new ElasticContainer(this);
    }
  }

  private ElasticContainer(Builder builder) {
    super(builder);
    this.waitForConnectivityAttempts = builder.maxReadyAttempts;
  }

  /**
   * Return the endpoint URL for the container.
   */
  public String endpointUrl() {
    return String.format("http://%s:%s/", config.getHost(), config.getPort());
  }

  @Override
  boolean checkConnectivity() {
    try {
      return readUrlContent(endpointUrl()).contains("docker-cluster");
    } catch (IOException e) {
      return false;
    }
  }

  protected ProcessBuilder runProcess() {
    List<String> args = dockerRun();
    args.add("-e");
    args.add("discovery.type=single-node");
    args.add("-e");
    args.add("xpack.security.enabled=false");
    args.add(config.image());
    return createProcessBuilder(args);
  }

}
