package io.ebean.test.containers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

class SqlServerContainerTest {

  static final String SQLSERVER_VER = "2019-CU15-ubuntu-20.04";

  @Test
  void start() {
    SqlServerContainer container = SqlServerContainer.builder(SQLSERVER_VER)
      .containerName("temp_sqlserver")
      .collation("SQL_Latin1_General_CP1_CS_AS")
      .port(11433)
      .build();

    //container.startWithCreate();
    //container.startContainerOnly();
    container.startWithDropCreate();
    container.stopRemove();
  }

  @Disabled
  @Test
  void start_when_defaultCollation() {
    SqlServerContainer container = SqlServerContainer.builder(SQLSERVER_VER)
      .containerName("temp_sqlserver")
      .port(2433)
      .collation("default")
      .build();

    container.startWithCreate();
    container.stopRemove();
  }

  @Disabled
  @Test
  void start_when_explicitDefaultCollation() {
    SqlServerContainer container = SqlServerContainer.builder(SQLSERVER_VER)
      .containerName("temp_sqlserver")
      .port(2433)
      .collation("SQL_Latin1_General_CP1_CI_AS")
      .build();

    container.startWithCreate();
    container.stopRemove();
  }

  @Disabled
  @Test
  void start_when_noCollation() {
    SqlServerContainer container = SqlServerContainer.builder(SQLSERVER_VER)
      .containerName("temp_sqlserver")
      .port(2433)
      .build();

    container.startWithCreate();
    container.stopRemove();
  }

  @Disabled
  @Test
  void start_when_explicitCollation() {
    SqlServerContainer container = SqlServerContainer.builder(SQLSERVER_VER)
      .containerName("temp_sqlserver")
      .port(2433)
      .collation("SQL_Latin1_General_CP1_CS_AS")
      .build();

    container.startWithCreate();
    container.stopRemove();
  }

  /**
   * When Collation is changed, the server sometimes will not start with dropCreate, because there are async
   * processes on container start. We must ensure, that we wait until container is started.
   * <p>
   * It is very hard to reproduce, I could only reproduce it with "startWithDropCreate" and an active
   * screen sharing session... So the test may look a bit messy to get the correct timing.
   * <p>
   * I observed the output:
   * 16:14:42.726 [main] DEBUG io.ebean.test.containers - connectivity confirmed for temp_sqlserver
   * <p>
   * comparing the timestamp with the docker-logs, the server was in the middle of collation change process.
   */
  @Disabled
  @Test
  void start_dropCreate_explicitCollationTimmingTest() throws Exception {
    SqlServerContainer container = SqlServerContainer.builder(SQLSERVER_VER)
      .containerName("temp_sqlserver")
      .port(2433)
      .collation("Latin1_General_100_BIN2")
      .build();
    for (int i = 0; i < 10; i++) {
      container.startWithDropCreate();
      try (Connection conn = DriverManager.getConnection(container.jdbcUrl(), container.dbConfig.getUsername(), container.dbConfig.getPassword())) {
        for (int j = 0; j < 10; j++) {
          Statement stmt = conn.createStatement();
          stmt.execute("create table my_test" + j + " (id int)");
          conn.commit();
        }
        Thread.sleep(2000);
        container.stopRemove();
      }
    }
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
    container.startMaybe();

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
