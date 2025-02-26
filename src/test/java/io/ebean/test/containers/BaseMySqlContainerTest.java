package io.ebean.test.containers;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BaseMySqlContainerTest {

  @Test
  void toDatabaseNames() {
    List<String> names = BaseMySqlContainer.toDatabaseNames("foo");
    assertThat(names).containsExactly("foo");

    names = BaseMySqlContainer.toDatabaseNames("foo,");
    assertThat(names).containsExactly("foo");
    names = BaseMySqlContainer.toDatabaseNames(",foo,");
    assertThat(names).containsExactly("foo");
    names = BaseMySqlContainer.toDatabaseNames(",foo");
    assertThat(names).containsExactly("foo");
  }

  @Test
  void toDatabaseNames_multiple() {
    List<String> names = BaseMySqlContainer.toDatabaseNames(" foo , bar ");
    assertThat(names).containsExactly("foo", "bar");

    names = BaseMySqlContainer.toDatabaseNames("foo,bar");
    assertThat(names).containsExactly("foo", "bar");
    names = BaseMySqlContainer.toDatabaseNames(",foo,bar,");
    assertThat(names).containsExactly("foo", "bar");
    names = BaseMySqlContainer.toDatabaseNames("  ,  foo  ,  bar  ,  ");
    assertThat(names).containsExactly("foo", "bar");
  }
}
