package io.ebean.docker.commands;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class YugabyteContainerTest {

  @Test
  void start_run_stop() {
    YugabyteConfig config = new YugabyteConfig("2.11.2.0-b89");
    config.setContainerName("temp_yugabyte");
    config.setExtensions("pgcrypto");
    config.setPort(9844);

    YugabyteContainer yugaContainer = new YugabyteContainer(config);
    yugaContainer.stopRemove();
    yugaContainer.startWithDropCreate();

    try {
      Connection connection = config.createConnection();
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
