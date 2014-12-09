package org.gbif.registry.guice;

import org.gbif.api.model.common.DOI;
import org.gbif.doi.service.DoiService;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

/**
 * Provides a mock doi service
 */
public class DoiMockModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(DoiService.class).toInstance(new DoiServiceMock());
    bind(String.class).annotatedWith(Names.named("doi.prefix")).toInstance(DOI.TEST_PREFIX);
  }
}
