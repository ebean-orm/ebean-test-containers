package org.avaje.docker.commands;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

public class ElasticContainerTest {

  @Test
  public void runProcess() throws Exception {

    ElasticConfig config = new ElasticConfig("5.6.0", new Properties());

    ElasticContainer elastic = new ElasticContainer(config);
    elastic.start();

    elastic.stop();
  }

}
