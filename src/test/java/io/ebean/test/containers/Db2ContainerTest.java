package io.ebean.test.containers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

class Db2ContainerTest {

  // run manually as it is soo slow
  @Disabled
  @Test
  void start() {
    Db2Container container = Db2Container.builder("11.5.8.0")
      .containerName("temp_db2")
      .port(50050)
      .fastStartMode(true)
      .build();

    container.startWithCreate();
    container.startContainerOnly();
    container.startWithDropCreate();

    container.stopRemove();
    container.dockerSu()
  }

  @Disabled
  @Test
  void viaContainerFactory() {
    Properties properties = new Properties();
    properties.setProperty("db2.version", "11.5.8.0");
    properties.setProperty("db2.containerName", "temp_db2");
    properties.setProperty("db2.port", "50050");

    properties.setProperty("db2.name", "test_roberto");
    properties.setProperty("db2.user", "test_robino");
    //properties.setProperty("db2.startMode", "dropCreate");
    properties.setProperty("db2.createOptions", "USING CODESET UTF-8 TERRITORY DE COLLATE USING IDENTITY PAGESIZE 32768");
    properties.setProperty("db2.configOptions", "USING STRING_UNITS CODEUNITS32");

    ContainerFactory factory = new ContainerFactory(properties);

    factory.startContainers(s -> System.out.println(">> " + s));

    ContainerConfig config = factory.config("db2");

    try {
      Connection connection = config.createConnection();

      exeSql(connection, "drop table if exists test_junk2");
      exeSql(connection, "create table test_junk2 (acol integer)");
      exeSql(connection, "insert into test_junk2 (acol) values (42)");
      exeSql(connection, "insert into test_junk2 (acol) values (43)");

      connection.close();

    } catch (SQLException e) {
      throw new RuntimeException(e);

    } finally {
      factory.stopContainers(s -> System.out.println(">> " + s));
    }
  }

  private void exeSql(Connection connection, String sql) throws SQLException {
    try (PreparedStatement st = connection.prepareStatement(sql)) {
      System.out.println("executed " + sql);
      st.execute();
    }
  }
}
