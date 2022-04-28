package io.ebean.docker.commands;

import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class RedisContainer extends BaseContainer {

  /**
   * Create the RedisContainer with configuration via properties.
   */
  public static RedisContainer create(String redisVersion, Properties properties) {
    return new RedisContainer(new RedisConfig(redisVersion, properties));
  }

  public RedisContainer(RedisConfig config) {
    super(config);
  }

  @Override
  boolean checkConnectivity() {

    String osName = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
    if (osName.indexOf("win") > -1) {
      return doWindowsCheck();
    }

    try {
      ProcessBuilder pb = new ProcessBuilder("nc", config.getHost(), Integer.toString(config.getPort()), "-q","0");
      pb.redirectErrorStream(true);

      Process process = pb.start();

      OutputStreamWriter ow = new OutputStreamWriter(process.getOutputStream());
      ow.write("PING");
      ow.flush();
      ow.close();

      return process.waitFor() == 0;

    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  private boolean doWindowsCheck() {
    try {
      // Oh well ...
      Thread.sleep(20);
      return true;
    } catch (Exception e) {
      return true;
    }
  }

  protected ProcessBuilder runProcess() {

    List<String> args = dockerRun();
    args.add(config.image());
    return createProcessBuilder(args);
  }

}
