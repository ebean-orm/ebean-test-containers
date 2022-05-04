package io.ebean.docker.commands;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

class NuoDBContainerTest {

  private static final Logger log = LoggerFactory.getLogger(NuoDBContainerTest.class);

  @Disabled
  @Test
  void start_executeSql_stop() {

    NuoDBContainer container = NuoDBContainer.builder("4.0")
    //config.setContainerName("nuodb");
    //config.setAdminUser("dba");
    //config.setAdminPassword("dba");
    //config.setDbName("testdb");
      .schema("my_app2")
      .user("my_app2")
      .password("test")
      .build();

    container.startWithDropCreate();

    try (Connection connection = container.createConnection()) {
      final Random random = new Random();

      exeSql(connection, "drop table if exists test_junk");
      exeSql(connection, "create table test_junk (acol integer)");
      exeSql(connection, "insert into test_junk (acol) values (" + random.nextInt() + ")");
      exeSql(connection, "insert into test_junk (acol) values (" + random.nextInt() + ")");
      exeSql(connection, "insert into test_junk (acol) values (" + random.nextInt() + ")");

      connection.commit();

    } catch (SQLException e) {
      throw new RuntimeException(e);

    } finally {
//      container.stop();
//      container.stopRemove();
    }
  }

  private void exeSql(Connection connection, String sql) throws SQLException {
    try (PreparedStatement st = connection.prepareStatement(sql)) {
      log.debug("execute SQL {}", sql);
      st.execute();
    }
  }

}
