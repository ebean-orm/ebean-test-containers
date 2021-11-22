package io.ebean.docker.commands;

import io.ebean.docker.commands.process.ProcessHandler;
import io.ebean.docker.container.Container;
import io.ebean.docker.container.ContainerConfig;
import io.ebean.docker.container.ContainerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import static org.assertj.core.api.Assumptions.assumeThat;
import static org.junit.jupiter.api.Assertions.*;

public class HanaContainerTest {

  private Path tempDir;
  private Path passwordsFile;

  @BeforeEach
  public void setUp() throws IOException {

    if (!System.getProperty("os.name").toLowerCase().contains("linux")) {
      this.passwordsFile = Paths.get("dummy.json");
      this.tempDir = Paths.get("dummy");
      return;
    }

    this.tempDir = Files.createTempDirectory("hana_docker");
    Files.setPosixFilePermissions(this.tempDir, PosixFilePermissions.fromString("rwxrwxrwx"));
    this.passwordsFile = this.tempDir.resolve("passwords.json");
    Files.createFile(this.passwordsFile);
    Files.setPosixFilePermissions(this.passwordsFile, PosixFilePermissions.fromString("rwxrwxrwx"));
    try (BufferedWriter bw = Files.newBufferedWriter(this.passwordsFile)) {
      bw.write("{\n");
      bw.write("    \"master_password\" : \"HXEHana1\"\n");
      bw.write("}");
    }
  }

  @AfterEach
  public void tearDown() throws IOException {

    Files.walkFileTree(this.tempDir, new FileVisitor<Path>() {

      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }

    });
  }

  // Setting ignore only because running this test takes a bit too long
  @Disabled
  @Test
  public void start() {

    assumeThat(System.getProperty("os.name").toLowerCase()).contains("linux");
    //System.setProperty("hana.agreeToSapLicense", "true");

    HanaConfig config = new HanaConfig("2.00.033.00.20180925.2");
    config.setPort(39117);
    config.setInstanceNumber("91");
    try {
      config.setPasswordsUrl(new URL("file:///hana/mounts/" + this.passwordsFile.getFileName()));
    } catch (MalformedURLException e) {
      fail(e.getMessage());
    }
    config.setMountsDirectory(this.tempDir.toString());
    config.setAgreeToSapLicense(HanaConfig.checkLicenseAgreement());
    HanaContainer container = new HanaContainer(config);

    assertTrue(container.startWithCreate());
    assertTrue(container.startContainerOnly());
    assertTrue(container.startWithDropCreate());

    cleanupContainer(config);
    container.stopRemove();
  }

  // Setting ignore only because running this test takes a bit too long
  @Disabled
  @Test
  public void viaContainerFactory() {

    assumeThat(System.getProperty("os.name").toLowerCase()).contains("linux");
    //System.setProperty("hana.agreeToSapLicense", "true");

    Properties properties = new Properties();
    properties.setProperty("hana.version", "2.00.033.00.20180925.2");
    properties.setProperty("hana.containerName", "dummy_hana");
    properties.setProperty("hana.port", "39217");

    properties.setProperty("hana.dbName", "HXE");
    properties.setProperty("hana.dbUser", "HXE");
    properties.setProperty("hana.startMode", "dropcreate");
    properties.setProperty("hana.instanceNumber", "92");
    properties.setProperty("hana.passwordsUrl", "file:///hana/mounts/" + this.passwordsFile.getFileName());
    properties.setProperty("hana.mountsDirectory", this.tempDir.toString());
    properties.setProperty("hana.agreeToSapLicense", Boolean.toString(HanaConfig.checkLicenseAgreement()));
    // properties.setProperty("hana.dbPassword", "test");

    ContainerFactory factory = new ContainerFactory(properties);
    factory.startContainers();

    Container container = factory.container("hana");
    ContainerConfig config = container.config();

    config.setStartMode(StartMode.DropCreate);
    assertTrue(container.start());

    config.setStartMode(StartMode.Container);
    assertTrue(container.start());

    config.setStartMode(StartMode.Create);
    assertTrue(container.start());

    try (Connection connection = config.createConnection();) {
      try {
        exeSql(connection, "drop table my_table");
      } catch (SQLException e) {
        // ignore
      }
      exeSql(connection, "create column table my_table (a integer)");
      exeSql(connection, "insert into my_table (a) values (42)");
      exeSql(connection, "insert into my_table (a) values (43)");

    } catch (SQLException e) {
      fail(e.getMessage());
    } finally {
      cleanupContainer((HanaConfig) config);
      ((HanaContainer) container).stopRemove();
    }
  }

  @Test
  public void noLicense() {

    HanaConfig config = new HanaConfig("2.00.033.00.20180925.2");
    config.setPort(39117);
    config.setInstanceNumber("91");
    try {
      config.setPasswordsUrl(new URL("file:///hana/mounts/" + this.passwordsFile.getFileName()));
    } catch (MalformedURLException e) {
      fail(e.getMessage());
    }
    config.setMountsDirectory(this.tempDir.toString());
    config.setAgreeToSapLicense(false);
    assertThrows(IllegalStateException.class, () -> new HanaContainer(config));
  }

  @Test
  public void noMountsDirectory() {

    Properties properties = new Properties();
    properties.setProperty("hana.version", "2.00.033.00.20180925.2");
    properties.setProperty("hana.containerName", "dummy_hana");
    properties.setProperty("hana.port", "39217");

    properties.setProperty("hana.dbName", "HXE");
    properties.setProperty("hana.dbUser", "HXE");
    properties.setProperty("hana.startMode", "dropcreate");
    properties.setProperty("hana.instanceNumber", "92");
    properties.setProperty("hana.passwordsUrl", "file:///hana/mounts/" + this.passwordsFile.getFileName());
    properties.setProperty("hana.mountsDirectory", "does not exist");

    assertThrows(IllegalArgumentException.class, () -> new ContainerFactory(properties));
  }

  private void exeSql(Connection connection, String sql) throws SQLException {
    try (PreparedStatement st = connection.prepareStatement(sql)) {
      st.execute();
    }
  }

  private void cleanupContainer(HanaConfig config) {
    ProcessHandler.command(config.getDocker(), "exec", "-i", config.containerName(), "bash", "-c",
      "rm -rf /hana/mounts/*");
  }
}
