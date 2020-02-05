package io.ebean.docker.commands;

import java.util.Properties;

public class ElasticConfig extends BaseConfig {

  public ElasticConfig(String version, Properties properties) {
    super("elastic", 9201, 9200, version);
    this.image = "docker.elastic.co/elasticsearch/elasticsearch:" + version;
    setProperties(properties);
  }
}
