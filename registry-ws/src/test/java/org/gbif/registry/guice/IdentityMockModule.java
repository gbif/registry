package org.gbif.registry.guice;

import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.api.service.common.IdentityService;

import java.util.Collections;
import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import static org.gbif.ws.server.filter.AppIdentityFilter.APPKEYS_WHITELIST;

/**
 * Mock Identity module to ease testing.
 * Intended to be use in IT and unit tests only.
 * Only allows to login using test users in {@link IdentityServiceMock} and reject all other operations.
 */
public class IdentityMockModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(IdentityService.class).to(IdentityServiceMock.class);
    bind(IdentityAccessService.class).to(IdentityServiceMock.class);
    //do not accept appkeys
    bind(new TypeLiteral<List<String>>() {
    }).annotatedWith(Names.named(APPKEYS_WHITELIST)).toInstance(
            Collections.emptyList());
  }
}
