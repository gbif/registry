package org.gbif.registry.doi;

import org.gbif.doi.service.DoiService;

import com.google.inject.AbstractModule;

/**
 * Provides a mock doi service
 */
public class DoiMockModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(DoiService.class).toInstance(new DoiServiceMock());
  }
}
