package org.avaje.docker.commands;

public class MySqlCommands implements DbCommands {

  private DbConfig config;

  public MySqlCommands(DbConfig config) {
    this.config = config;
  }

  @Override
  public boolean start() {
    return false;
  }

  @Override
  public void stop() {

  }

  @Override
  public String getStartDescription() {
    return config.getStartDescription();
  }

  @Override
  public String getStopDescription() {
    return config.getStopDescription();
  }

}
