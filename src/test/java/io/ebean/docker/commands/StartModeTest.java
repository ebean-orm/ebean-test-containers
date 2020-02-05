package io.ebean.docker.commands;

import org.junit.Test;

import static org.junit.Assert.*;

public class StartModeTest {

  @Test
  public void of_container() {
    assertEquals(StartMode.Container, StartMode.of("conTainer"));
    assertEquals(StartMode.Container, StartMode.of("container"));
    assertEquals(StartMode.Container, StartMode.of("CONTAINER"));
  }

  @Test
  public void of_dropCreate() {
    assertEquals(StartMode.DropCreate, StartMode.of("dropCreate"));
    assertEquals(StartMode.DropCreate, StartMode.of("DROPCREATE"));
    assertEquals(StartMode.DropCreate, StartMode.of("DropCreate"));
  }

  @Test
  public void of_create() {
    assertEquals(StartMode.Create, StartMode.of("junk"));
    assertEquals(StartMode.Create, StartMode.of(""));
    assertEquals(StartMode.Create, StartMode.of("create"));
    assertEquals(StartMode.Create, StartMode.of("Create"));
  }
}
