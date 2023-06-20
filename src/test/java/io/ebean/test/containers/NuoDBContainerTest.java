package io.ebean.test.containers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

import static java.lang.System.Logger.Level.DEBUG;

class NuoDBContainerTest {

  private static final System.Logger log = System.getLogger("io.ebean.test.containers.NuoDBContainerTest");

  @Disabled
  @Test
  void start_executeSql_stop() {
    NuoDBContainer container = NuoDBContainer.builder("4.3.2")
      //.port(48004)
      //.adminPort(8888)
      //.containerName("nuodb")
      //.adminUser("dba")
      //.adminPassword("dba")
      .schema("test_user")
      .user("test_user")
      .password("test")
      .build();

    container.startMaybe();

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
    }
  }

  private void exeSql(Connection connection, String sql) throws SQLException {
    try (PreparedStatement st = connection.prepareStatement(sql)) {
      log.log(DEBUG, "execute SQL {0}", sql);
      st.execute();
    }
  }

}
