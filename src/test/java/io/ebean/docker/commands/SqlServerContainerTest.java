package io.ebean.docker.commands;

import io.ebean.docker.container.Container;
import io.ebean.docker.container.ContainerConfig;
import io.ebean.docker.container.ContainerFactory;
import io.ebean.docker.container.StopMode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

class SqlServerContainerTest {

  private static final Logger log = LoggerFactory.getLogger(SqlServerContainerTest.class);

  static final String SQLSERVER_VER = "2019-CU15-ubuntu-20.04";

  @Test
  void start() {
    SqlServerContainer container = SqlServerContainer.newBuilder(SQLSERVER_VER)
      .setContainerName("temp_sqlserver")
      .setCollation("SQL_Latin1_General_CP1_CS_AS")
      .setPort(11433)
      .setStopMode(StopMode.Remove)
      .build();
    //config.setFastStartMode(true);

    //container.startWithCreate();
    //container.startContainerOnly();
    container.startWithDropCreate();
    container.stopRemove();
  }

  @Disabled
  @Test
  void start_when_defaultCollation() {
    SqlServerContainer container = SqlServerContainer.newBuilder(SQLSERVER_VER)
      .setContainerName("temp_sqlserver")
      .setPort(2433)
      .setCollation("default")
      .build();

    container.startWithCreate();
    container.stopRemove();
  }

  @Disabled
  @Test
  void start_when_noCollation() {
    SqlServerContainer container = SqlServerContainer.newBuilder(SQLSERVER_VER)
      .setContainerName("temp_sqlserver")
      .setPort(2433)
      .build();

    container.startWithCreate();
    container.stopRemove();
  }

  @Disabled
  @Test
  void start_when_explicitCollation() {
    SqlServerContainer container = SqlServerContainer.newBuilder(SQLSERVER_VER)
      .setContainerName("temp_sqlserver")
      .setPort(2433)
      .setCollation("SQL_Latin1_General_CP1_CS_AS")
      .build();

    container.startWithCreate();
    container.stopRemove();
  }

  @Disabled
  @Test
  void viaContainerFactory() {
    Properties properties = new Properties();
    properties.setProperty("sqlserver.version", SQLSERVER_VER);
    properties.setProperty("sqlserver.containerName", "temp2_sqlserver");
    properties.setProperty("sqlserver.port", "2433");

    properties.setProperty("sqlserver.dbName", "test_other");
    properties.setProperty("sqlserver.dbUser", "test_robino");
    properties.setProperty("sqlserver.startMode", "dropcreate");
    //properties.setProperty("sqlserver.dbPassword", "test");

    ContainerFactory factory = new ContainerFactory(properties);
    factory.startContainers();

    Container container = factory.container("sqlserver");
    ContainerConfig config = container.config();

    //config.setStartMode(StartMode.DropCreate);
    //config.setStopMode(StopMode.Remove);
    container.start();

//    config.setStartMode(StartMode.Container);
//    container.start();
//
//    config.setStartMode(StartMode.Create);
//    config.setStopMode(StopMode.Remove);
//    container.start();

    try {
      Connection connection = config.createConnection();

      exeSql(connection, "drop table if exists test_junk");
      exeSql(connection, "create table test_junk (acol integer)");
      exeSql(connection, "insert into test_junk (acol) values (42)");
      exeSql(connection, "insert into test_junk (acol) values (43)");

      log.info("executed test sql scripts");

    } catch (SQLException e) {
      throw new RuntimeException(e);

    } finally {
      container.stop();
    }
  }

  private void exeSql(Connection connection, String sql) throws SQLException {
    PreparedStatement st = connection.prepareStatement(sql);
    st.execute();
    st.close();
  }
}
