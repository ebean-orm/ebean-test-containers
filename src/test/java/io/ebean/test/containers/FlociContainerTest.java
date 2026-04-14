package io.ebean.test.containers;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class FlociContainerTest {

  @Test
  void defaults() {
    InternalConfig config = FlociContainer.builder("latest").internalConfig();
    config.setDefaultContainerName();

    assertThat(config.platform()).isEqualTo("floci");
    assertThat(config.getPort()).isEqualTo(4566);
    assertThat(config.getInternalPort()).isEqualTo(4566);
    assertThat(config.getImage()).isEqualTo("hectorvent/floci:latest");
    assertThat(config.containerName()).isEqualTo("ut_floci");
  }

  @Test
  void properties_with_noPrefix() {
    Properties properties = new Properties();
    properties.setProperty("floci.image", "foo");
    properties.setProperty("floci.port", "7380");
    properties.setProperty("floci.containerName", "floci_junk8");
    properties.setProperty("floci.internalPort", "5379");
    properties.setProperty("floci.startMode", "baz");
    properties.setProperty("floci.shutdownMode", "bar");
    properties.setProperty("floci.awsRegion", "us-east-1");

    FlociContainer.Builder builder = FlociContainer.builder("latest")
      .properties(properties);
    InternalConfig config = builder.internalConfig();
    assertProperties(config);

    FlociContainer container = builder.build();
    assertThat(container.awsRegion()).isEqualTo("us-east-1");
  }

  @Test
  void properties_with_ebeanTestPrefix() {
    Properties properties = new Properties();
    properties.setProperty("ebean.test.floci.image", "foo");
    properties.setProperty("ebean.test.floci.port", "7380");
    properties.setProperty("ebean.test.floci.containerName", "floci_junk8");
    properties.setProperty("ebean.test.floci.internalPort", "5379");
    properties.setProperty("ebean.test.floci.startMode", "baz");
    properties.setProperty("ebean.test.floci.shutdownMode", "bar");
    properties.setProperty("ebean.test.floci.awsRegion", "us-east-1");

    FlociContainer.Builder builder = FlociContainer.builder("latest")
      .properties(properties);
    InternalConfig config = builder.internalConfig();
    config.setDefaultContainerName();
    assertProperties(config);

    FlociContainer container = builder.build();
    assertThat(container.awsRegion()).isEqualTo("us-east-1");
  }

  @Test
  void sdk2() {
    FlociContainer container = FlociContainer.builder("latest")
      .awsRegion("us-east-1")
      .build();

    AwsSDKv2 sdk = container.sdk2();
    assertThat(container.sdk()).isNotNull();
    assertThat(sdk.endpoint()).isEqualTo(container.endpoint());
    assertThat(sdk.region().id()).isEqualTo("us-east-1");
    assertThat(sdk.basicCredentials().accessKeyId()).isEqualTo("localstack");
    assertThat(sdk.basicCredentials().secretAccessKey()).isEqualTo("localstack");
  }

  private void assertProperties(InternalConfig config) {
    assertThat(config.getPort()).isEqualTo(7380);
    assertThat(config.getInternalPort()).isEqualTo(5379);
    assertThat(config.getImage()).isEqualTo("foo");
    assertThat(config.getStartMode()).isEqualTo(StartMode.Create);
    assertThat(config.shutdownMode()).isEqualTo(StopMode.Stop);
    assertThat(config.containerName()).isEqualTo("floci_junk8");
  }
}
