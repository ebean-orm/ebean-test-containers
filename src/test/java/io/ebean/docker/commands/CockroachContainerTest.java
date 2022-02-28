package io.ebean.docker.commands;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

class CockroachContainerTest {

  @Disabled
  @Test
  void start() throws SQLException {

    CockroachConfig config = new CockroachConfig();//"v21.2.4");
    //config.setContainerName("junk_roach");
    config.setDbName("unit");
    //config.setUser("test_roach");
    //config.setPassword("test");

    CockroachContainer container = new CockroachContainer(config);
    container.startWithDropCreate();

    try (Connection connection = container.createConnection()) {
      try (Statement statement = connection.createStatement()) {
        statement.execute("drop table if exists foobar2");
        statement.execute("create table foobar2 (acol integer)");
        statement.execute("insert into foobar2 (acol) values (42)");
      }
    }

    container.stopRemove();
  }

}
