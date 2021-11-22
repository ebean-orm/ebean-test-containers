package io.ebean.docker.commands;

import io.ebean.docker.container.Container;
import io.ebean.docker.container.ContainerConfig;
import io.ebean.docker.container.ContainerFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

public class SqlServerContainerTest {

  private static final Logger log = LoggerFactory.getLogger(SqlServerContainerTest.class);

  static final String SQLSERVER_VER = "2019-GA-ubuntu-16.04";

  @Disabled
  @Test
  public void start_when_defaultCollation() {

    SqlServerConfig config = new SqlServerConfig(SQLSERVER_VER);
    config.setContainerName("temp_sqls");
    config.setPort(2433);
    config.setCollation("default");

    SqlServerContainer container = new SqlServerContainer(config);

    container.startWithCreate();
    container.stopRemove();
  }

  @Disabled
  @Test
  public void start_when_noCollation() {

    SqlServerConfig config = new SqlServerConfig(SQLSERVER_VER);
    config.setContainerName("temp_sqls");
    config.setPort(2433);

    SqlServerContainer container = new SqlServerContainer(config);

    container.startWithCreate();
    container.stopRemove();
  }

  @Disabled
  @Test
  public void start_when_explicitCollation() {

    SqlServerConfig config = new SqlServerConfig(SQLSERVER_VER);
    config.setContainerName("temp_sqls");
    config.setPort(2433);
    config.setCollation("SQL_Latin1_General_CP1_CS_AS");

    SqlServerContainer container = new SqlServerContainer(config);

    container.startWithCreate();
    container.stopRemove();
  }

  @Test
  public void start() {

    SqlServerConfig config = new SqlServerConfig(SQLSERVER_VER);
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
    properties.setProperty("sqlserver.version", SQLSERVER_VER);
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

    config.setStartMode(StartMode.DropCreate);
    config.setStopMode(StopMode.Remove);
    container.start();

    config.setStartMode(StartMode.Container);
    container.start();

    config.setStartMode(StartMode.Create);
    container.start();

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
