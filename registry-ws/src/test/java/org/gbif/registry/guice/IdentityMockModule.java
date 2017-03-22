package org.gbif.registry.guice;

import org.gbif.api.service.common.IdentityService;

import com.google.inject.AbstractModule;

/**
 * Mock Identity module to ease testing.
 * Intended to be use in IT and unit tests only.
 */
public class IdentityMockModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(IdentityService.class).to(IdentityServiceMock.class);
  }
}
