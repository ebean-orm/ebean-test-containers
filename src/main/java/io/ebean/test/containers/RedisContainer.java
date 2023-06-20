package io.ebean.test.containers;

import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Locale;

public class RedisContainer extends BaseContainer {

  @Override
  public RedisContainer start() {
    startOrThrow();
    return this;
  }

  /**
   * Create a builder for RedisContainer.
   */
  public static Builder builder(String version) {
    return new Builder(version);
  }

  /**
   * Deprecated - migrate to builder().
   */
  @Deprecated
  public static Builder newBuilder(String version) {
    return builder(version);
  }

  /**
   * The RedisContainer builder.
   */
  public static class Builder extends BaseConfig<RedisContainer, RedisContainer.Builder> {

    private Builder(String version) {
      super("redis", 6379, 6379, version);
    }

    @Override
    public RedisContainer build() {
      return new RedisContainer(this);
    }

    @Override
    public RedisContainer start() {
      return build().start();
    }
  }

  private RedisContainer(Builder builder) {
    super(builder);
  }

  @Override
  boolean checkConnectivity() {
    String os = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
    if (os.contains("win")) {
      return doWindowsCheck();
    }
    try {
      ProcessBuilder pb;
      if (os.contains("mac")) {
        pb = new ProcessBuilder("nc", config.getHost(), Integer.toString(config.getPort()));
      } else {
        pb = new ProcessBuilder("nc", config.getHost(), Integer.toString(config.getPort()), "-q", "0");
      }
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
