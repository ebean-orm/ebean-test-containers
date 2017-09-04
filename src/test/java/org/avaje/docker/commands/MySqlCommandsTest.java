package org.avaje.docker.commands;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public class MySqlCommandsTest {

  @Test
  public void basic() throws InterruptedException {


    Properties properties = new Properties();
    properties.setProperty("dbPlatform", "mysql");
    properties.setProperty("dbContainerName", "temp_mysql");
    properties.setProperty("dbPort", "7306");

    properties.setProperty("dbName", "test_roberto");
    properties.setProperty("dbUser", "test_robino");

    DbConfig config = DbConfigFactory.create(properties);
    DbCommands mysql = DbConfigFactory.createCommands(config);

    //config.dbStartMode = "dropCreate";
    mysql.start();


    String url = "jdbc:mysql://localhost:" + config.dbPort + "/" + config.dbName;

    try {
      Connection connection = DriverManager.getConnection(url, config.dbUser, config.dbPassword);

      exeSql(connection, "drop table if exists test_junk2");
      exeSql(connection, "create table test_junk2 (acol integer)");
      exeSql(connection, "insert into test_junk2 (acol) values (42)");
      exeSql(connection, "insert into test_junk2 (acol) values (43)");

    } catch (SQLException e) {
      throw new RuntimeException(e);

    } finally {
      mysql.stop();
    }
  }

  private void exeSql(Connection connection, String sql) throws SQLException {
    PreparedStatement st = connection.prepareStatement(sql);
    st.execute();
    st.close();
  }
}
