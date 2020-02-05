package io.ebean.docker.commands;

import org.junit.Test;

import static org.junit.Assert.*;

public class StopModeTest {

  @Test
  public void of() {
    assertEquals(StopMode.Remove, StopMode.of("Remove"));
    assertEquals(StopMode.Remove, StopMode.of("REMOVE"));

    assertEquals(StopMode.None, StopMode.of("None"));
    assertEquals(StopMode.None, StopMode.of("NONE"));

    assertEquals(StopMode.Stop, StopMode.of("junk"));
    assertEquals(StopMode.Stop, StopMode.of("Stop"));
    assertEquals(StopMode.Stop, StopMode.of("STOP"));
  }
}
