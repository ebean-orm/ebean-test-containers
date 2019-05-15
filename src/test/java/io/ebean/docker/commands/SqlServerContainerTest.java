package io.ebean.docker.commands;

import io.ebean.docker.container.ContainerConfig;
import io.ebean.docker.container.ContainerFactory;
import io.ebean.docker.container.Container;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public class SqlServerContainerTest {

  @Ignore
  @Test
  public void start_when_defaultCollation() {

    SqlServerConfig config = new SqlServerConfig("2017-CU4");
    config.setContainerName("temp_sqls");
    config.setPort("2433");
    config.setCollation("defaullt");

    SqlServerContainer container = new SqlServerContainer(config);

    container.startWithCreate();
    //container.stopRemove();
  }

  @Ignore
  @Test
  public void start_when_noCollation() {

    SqlServerConfig config = new SqlServerConfig("2017-CU4");
    config.setContainerName("temp_sqls");
    config.setPort("2433");

    SqlServerContainer container = new SqlServerContainer(config);

    container.startWithCreate();
    //container.stopRemove();
  }

  @Ignore
  @Test
  public void start_when_explicitCollation() {

    SqlServerConfig config = new SqlServerConfig("2017-CU4");
    config.setContainerName("temp_sqls");
    config.setPort("2433");
    config.setCollation("SQL_Latin1_General_CP1_CS_AS");

    SqlServerContainer container = new SqlServerContainer(config);

    container.startWithCreate();
    //container.stopRemove();
  }

  @Test
  public void start() {

    SqlServerConfig config = new SqlServerConfig("2017-CU4");
    config.setFastStartMode(true);

    SqlServerContainer container = new SqlServerContainer(config);

    container.startWithCreate();
    container.startContainerOnly();
    container.startWithDropCreate();

    //container.stopOnly();
  }

  @Test
  public void viaContainerFactory() {

    Properties properties = new Properties();
    properties.setProperty("sqlserver.version", "2017-CU2");
    properties.setProperty("sqlserver.containerName", "junk_sqlserver");
    properties.setProperty("sqlserver.port", "2433");

    properties.setProperty("sqlserver.dbName", "test_other");
    properties.setProperty("sqlserver.dbUser", "test_robino");
    properties.setProperty("sqlserver.startMode", "dropcreate");
    //properties.setProperty("sqlserver.dbPassword", "test");

    ContainerFactory factory = new ContainerFactory(properties);
    factory.startContainers();

    Container container = factory.container("sqlserver");
    ContainerConfig config = container.config();

    config.setStartMode("dropCreate");
    container.start();

    config.setStartMode("container");
    container.start();

    config.setStartMode("create");
    container.start();

    try {
      Connection connection = config.createConnection();

      exeSql(connection, "drop table if exists test_junk");
      exeSql(connection, "create table test_junk (acol integer)");
      exeSql(connection, "insert into test_junk (acol) values (42)");
      exeSql(connection, "insert into test_junk (acol) values (43)");

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
