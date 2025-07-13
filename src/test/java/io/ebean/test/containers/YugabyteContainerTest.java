package io.ebean.test.containers;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class YugabyteContainerTest {

  @Test
  void start_run_stop() {
    YugabyteContainer yugaContainer = YugabyteContainer.builder("2.20.11.0-b34")
      .containerName("temp_yugabyte")
      .extensions("pgcrypto")
      .port(9844)
      .port7000(7001)
      .build();

    yugaContainer.stopRemove();
    yugaContainer.startWithDropCreate();

    try {
      Connection connection = yugaContainer.createConnection();
      exeSql(connection, "create table test_junk2 (acol integer, aname varchar(20))");
      exeSql(connection, "insert into test_junk2 (acol) values (42)");
      exeSql(connection, "insert into test_junk2 (acol) values (43)");
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    yugaContainer.stopRemove();
  }

  private void exeSql(Connection connection, String sql) throws SQLException {
    PreparedStatement st = connection.prepareStatement(sql);
    st.execute();
    st.close();
  }
}
