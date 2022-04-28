package io.ebean.docker.container;

/**
 * Container builder - will move this after next step in refactor.
 */
public interface CBuilder<C, SELF extends CBuilder<C, SELF>> extends ContainerBuilderDb<SELF> {

  /**
   * Build the container.
   */
  C build();
}
