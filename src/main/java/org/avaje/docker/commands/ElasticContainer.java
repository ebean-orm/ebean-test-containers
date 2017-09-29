package org.avaje.docker.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ElasticContainer extends BaseContainer {

  /**
   * Create the ElasticContainer with configuration via properties.
   */
  public static ElasticContainer create(String elasticVersion, Properties properties) {
    return new ElasticContainer(new ElasticConfig(elasticVersion, properties));
  }

  //private final ElasticConfig elasticConfig;

  public ElasticContainer(ElasticConfig config) {
    super(config);
    //this.elasticConfig = config;
  }

  @Override
  boolean checkConnectivity() {

    try {
      URL url = new URL("http://localhost:"+config.getPort()+"/");
      URLConnection yc = url.openConnection();

      StringBuilder sb = new StringBuilder(300);
      try (BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()))) {
        String inputLine;
        while ((inputLine = in.readLine()) != null)
          sb.append(inputLine).append("\n");
      }
      return sb.toString().contains("docker-cluster");

    } catch (IOException e) {
      return false;
    }
  }

  protected ProcessBuilder runProcess() {

    //docker run -d -p 9200:9200 --name elasticsearch -e "http.host=0.0.0.0" -e "transport.host=127.0.0.1"
    // -e "xpack.security.enabled=false" docker.elastic.co/elasticsearch/elasticsearch:5.6.0

    List<String> args = new ArrayList<>();
    args.add(config.docker);
    args.add("run");
    args.add("-d");
    args.add("--name");
    args.add(config.containerName());
    args.add("-p");
    args.add(config.port + ":" + config.internalPort);

    args.add("-e");
    args.add("http.host=0.0.0.0");
    args.add("-e");
    args.add("transport.host=127.0.0.1");
    args.add("-e");
    args.add("xpack.security.enabled=false");

    //if (config.image != null) {
      args.add(config.image);

//    } else if (config.version != null) {
//      args.add(config.imageBase + ":" + config.version);
//    }

    return createProcessBuilder(args);
  }

}
