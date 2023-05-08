package io.ebean.test.containers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseConfigTest {

  @Test
  void imageWithMirror_nameOnly() {
    String val = BaseConfig.imageWithMirror("my.ecr/mirror", "redis:latest");
    assertThat(val).isEqualTo("my.ecr/mirror/docker.io/redis:latest");
  }

  @Test
  void imageWithMirror_pathName() {
    String val = BaseConfig.imageWithMirror("my.ecr/mirror", "localstack/localstack:3");
    assertThat(val).isEqualTo("my.ecr/mirror/docker.io/localstack/localstack:3");
  }

  @Test
  void imageWithMirror_localhostName() {
    String val = BaseConfig.imageWithMirror("my.ecr/mirror", "localhost/foo:4");
    assertThat(val).isEqualTo("localhost/foo:4");
  }

  @Test
  void imageWithMirror_repoPathName() {
    String val = BaseConfig.imageWithMirror("my.ecr/mirror", "my.repo/localstack/localstack:3");
    assertThat(val).isEqualTo("my.ecr/mirror/my.repo/localstack/localstack:3");
  }
}
