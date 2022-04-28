package io.ebean.docker.commands;

import io.ebean.docker.container.Container;
import io.ebean.docker.container.ContainerConfig;
import io.ebean.docker.container.ContainerFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public class OracleContainerTest {

  @Disabled
  @Test
  public void start() {

    OracleConfig config = new OracleConfig();
    config.setUser("test_ebean");
    //config.setContainerName("test_ebean_migration_oracle");

    OracleContainer container = new OracleContainer(config);

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

    //container.stopRemove();
  }

  @Disabled
  @Test
  public void viaContainerFactory() {

    Properties properties = new Properties();
    properties.setProperty("oracle.version", "latest");
    //properties.setProperty("oracle.containerName", "junk_oracle");
    //properties.setProperty("oracle.port", "1521");
    //properties.setProperty("oracle.dbName", "test_rob");

    properties.setProperty("oracle.dbUser", "test_robino");
    //properties.setProperty("oracle.startMode", "dropcreate");

    ContainerFactory factory = new ContainerFactory(properties);
    factory.startContainers();

    Container container = factory.container("oracle");
    ContainerConfig config = container.config();

    //config.setStartMode(StartMode.DropCreate);
    container.start();

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
