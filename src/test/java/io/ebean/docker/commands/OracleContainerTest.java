package io.ebean.docker.commands;

import io.ebean.docker.container.Container;
import io.ebean.docker.container.ContainerConfig;
import io.ebean.docker.container.ContainerFactory;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public class OracleContainerTest {

  @Ignore
  @Test
  public void start() {

    OracleConfig config = new OracleConfig("latest");
    config.setUser("test_start");
    //config.setPort("15221");
    OracleContainer container = new OracleContainer(config);

    container.startWithCreate();
    container.startContainerOnly();
    container.startWithDropCreate();

    //container.stopOnly();
  }

  @Ignore
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

    config.setStartMode("dropCreate");
    container.start();

    config.setStartMode("container");
    container.start();

    config.setStartMode("create");
    container.start();

    try {
      Connection connection = config.createConnection();
      exeSql(connection, "create table test_junk (acol integer)");
      exeSql(connection, "insert into test_junk (acol) values (42)");
      exeSql(connection, "insert into test_junk (acol) values (43)");

      // docker exec -it ut_oracle bash
      // $ORACLE_HOME/bin/sqlplus system/oracle
      // $ORACLE_HOME/bin/sqlplus test_robino/test

    } catch (SQLException e) {
      throw new RuntimeException(e);

    } finally {
      //container.stop();
    }
  }

  private void exeSql(Connection connection, String sql) throws SQLException {
    PreparedStatement st = connection.prepareStatement(sql);
    st.execute();
    st.close();
  }
}
