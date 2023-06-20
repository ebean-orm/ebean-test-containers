package io.ebean.test.containers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

class OracleContainerTest {

  @Disabled
  @Test
  void start() {
    OracleContainer container = OracleContainer.builder("21.3.0-slim")
      //.user("test_ebean")
      .build();

    if (!container.startWithDropCreate()) {
      throw new IllegalStateException("Failed to start?");
    }
    //container.startContainerOnly();
    //container.startWithDropCreate();

    try (Connection connection = container.createConnection()) {
      exeSql(connection, "create table test_junk (acol integer)");
      exeSql(connection, "insert into test_junk (acol) values (42)");
      exeSql(connection, "insert into test_junk (acol) values (43)");

      // docker exec -it ut_oracle bash
      // $ORACLE_HOME/bin/sqlplus system/oracle
      // $ORACLE_HOME/bin/sqlplus test_robino/test

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    // container.stop();
  }

  @Disabled
  @Test
  void viaContainerFactory() {
    Properties properties = new Properties();
    properties.setProperty("oracle.version", "21.3.0-slim");
    //properties.setProperty("oracle.containerName", "junk_oracle");
    //properties.setProperty("oracle.port", "1521");
    //properties.setProperty("oracle.dbName", "test_rob");

    properties.setProperty("oracle.dbUser", "test_robino");
    properties.setProperty("oracle.startMode", "dropcreate");

    ContainerFactory factory = new ContainerFactory(properties);
    factory.startContainers();

    Container container = factory.container("oracle");
    ContainerConfig config = container.config();

    //config.setStartMode(StartMode.DropCreate);
    container.startMaybe();

//    config.setStartMode(StartMode.Container);
//    container.start();
//
//    config.setStartMode(StartMode.Create);
//    container.start();

    try (Connection connection = config.createConnection()) {
      exeSql(connection, "create table test_junk (acol integer)");
      exeSql(connection, "insert into test_junk (acol) values (42)");
      exeSql(connection, "insert into test_junk (acol) values (43)");

      // docker exec -it ut_oracle bash
      // $ORACLE_HOME/bin/sqlplus system/oracle
      // $ORACLE_HOME/bin/sqlplus test_robino/test

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    //container.stop();
  }

  private void exeSql(Connection connection, String sql) throws SQLException {
    PreparedStatement st = connection.prepareStatement(sql);
    st.execute();
    st.close();
  }
}
