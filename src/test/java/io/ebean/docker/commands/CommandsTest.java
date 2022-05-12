package io.ebean.docker.commands;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommandsTest {

  @Test
  void parsePort() {
    assertThat(Commands.parsePort("4566/tcp -> 0.0.0.0:49153")).isEqualTo(49153);
    assertThat(Commands.parsePort("4566/tcp -> :::49153")).isEqualTo(49153);
    assertThat(Commands.parsePort("anything -> :a:b:1")).isEqualTo(1);
    assertThat(Commands.parsePort("anything -> :a:b:22")).isEqualTo(22);
  }

  @Test
  void parsePort_noArrow() {
    assertThat(Commands.parsePort("anything - 0.0.0.0:49153")).isEqualTo(0);
    assertThat(Commands.parsePort("4566/tcp -> 49153")).isEqualTo(0);
  }

  @Test
  void parsePort_noColon() {
    assertThat(Commands.parsePort("4566/tcp -> 49153")).isEqualTo(0);
  }

}
