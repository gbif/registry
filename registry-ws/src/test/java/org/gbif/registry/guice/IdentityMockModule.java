package org.gbif.registry.guice;

import org.gbif.api.service.common.UserService;

import com.google.inject.AbstractModule;

public class IdentityMockModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(UserService.class).to(UserServiceMock.class);
  }
}
