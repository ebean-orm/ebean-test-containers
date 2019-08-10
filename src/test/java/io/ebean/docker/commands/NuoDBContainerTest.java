package io.ebean.docker.commands;

import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

public class NuoDBContainerTest {

  @Test
  public void start_executeSql_stop() {

    NuoDBConfig config = new NuoDBConfig();
    //config.setContainerName("ut_nuodb");
    //config.setAdminUser("dba");
    //config.setAdminPassword("dba");
    config.setDbName("my_app");
    config.setSchema("my_app");
    config.setUser("my_app");
    config.setPassword("test");

    NuoDBContainer container = new NuoDBContainer(config);
    container.stopRemove();
    container.start();

    try (Connection connection = config.createConnection()) {
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
      container.stopRemove();
    }
  }

  private void exeSql(Connection connection, String sql) throws SQLException {
    try (PreparedStatement st = connection.prepareStatement(sql)) {
      st.execute();
    }
  }

}
