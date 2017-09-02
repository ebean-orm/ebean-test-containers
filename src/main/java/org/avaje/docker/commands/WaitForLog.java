package org.avaje.docker.commands;

/**
 * WaitForReady implementation that loops searching for a log entry in the
 * container to indicate the container is ready for use.
 */
public class WaitForLog { //implements WaitForReady {

//  private final String containerName;
//
//  private final String logMatch;
//
//  private int maxWait = 50;
//
//  private int sleepMillis = 100;
//
//  private int tail;
//
//  public WaitForLog(String containerName, String logMatch) {
//    this.containerName = containerName;
//    this.logMatch = logMatch;
//  }
//
//  /**
//   * Set the maxWait attempts for looking for the 'ready' log entry.
//   */
//  public WaitForLog withMaxWait(int maxWait) {
//    this.maxWait = maxWait;
//    return this;
//  }
//
//  /**
//   * Set the length of the tail of the logs to search in.
//   */
//  public WaitForLog withTail(int tail) {
//    this.tail = tail;
//    return this;
//  }
//
//  /**
//   * Set the sleep time between attempts.
//   */
//  public WaitForLog withSleepMillis(int sleepMillis) {
//    this.sleepMillis = sleepMillis;
//    return this;
//  }
//
//  /**
//   * Return true if the log match is found (container is deemed ready for use).
//   */
//  public boolean isReady() {
//    return commands.logsContain(containerName, logMatch, tail);
//  }
//
//  /**
//   * Wait checking log [tail] messages for the log entry match to be found.
//   * <p>
//   * Returns false if the log entry is not found in the given maxWait attempts.
//   * </p>
//   *
//   * @return True if the log entry is found and container deemed ready for use.
//   */
//  public boolean waitForReady() {
//    try {
//      for (int i = 0; i < maxWait; i++) {
//        if (isReady()) {
//          return true;
//        }
//        Thread.sleep(sleepMillis);
//      }
//      return false;
//
//    } catch (InterruptedException e) {
//      Thread.currentThread().interrupt();
//      return false;
//    }
//  }
}
