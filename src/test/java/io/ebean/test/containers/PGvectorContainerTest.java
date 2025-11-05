package io.ebean.test.containers;

import io.ebean.Database;
import io.ebean.datasource.DataSourcePool;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;

import com.pgvector.PGvector;

import static org.assertj.core.api.Assertions.assertThat;

public class PGvectorContainerTest {
  private final HashSet<Connection> connections = new HashSet<>();

  /**
   * Helper function to register the PGvector types only once per connection.
   * @param connection the connection
   * @return the same connection
   * @throws SQLException if an error occurs
   */
  private Connection wrapConnection(Connection connection) throws SQLException {
    if(connections.add(connection)) {
      PGvector.registerTypes(connection);
    }
    return connection;
  }

  @Test
  void extraDb() throws java.sql.SQLException {
    PGvectorContainer container = PGvectorContainer.builder("pg18")
      .port(0)
      .extraDb("myextra")
      .build();

    container.startMaybe();
    assertThat(container.port()).isGreaterThan(0);

    ContainerConfig containerConfig = container.config();
    assertThat(containerConfig.port()).isEqualTo(container.port());

    String jdbcUrl = container.config().jdbcUrl();
    assertThat(jdbcUrl).contains(":" + containerConfig.port());
    runSomeSql(container);

    DataSourcePool dataSource = container.ebean().dataSourceBuilder().build();
    try (Connection connection = wrapConnection(dataSource.getConnection())) {
      exeSql(connection, "INSERT INTO items (embedding) values ('[7,8,9]')");
    }
    dataSource.shutdown();

    Database ebean = container.ebean().builder()
      .register(false)
      .defaultDatabase(false)
      .build();
    // This can't be done yet, because Ebean doesn't know about PGvector type
//    ebean.sqlUpdate("insert into items (embedding) values (?)")
//      .setParameter(new PGvector(new float[] { 10f, 11f, 12f }))
//      .execute();

    ebean.shutdown();
  }

  private void runSomeSql(PGvectorContainer container) {
    try {
      Connection connection = wrapConnection(container.createConnection());
      exeSql(connection, "CREATE TABLE items (id bigserial PRIMARY KEY, embedding vector(3))");
      exeSql(connection, "INSERT INTO items (embedding) VALUES ('[1,2,3]'), ('[4,5,6]')");
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private static void exeSql(Connection connection, String sql) throws SQLException {
    try (PreparedStatement st = connection.prepareStatement(sql)) {
      st.execute();
    }
  }
}
