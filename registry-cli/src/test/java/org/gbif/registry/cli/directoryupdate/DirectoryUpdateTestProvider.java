package org.gbif.registry.cli.directoryupdate;

import org.gbif.api.service.directory.NodeService;
import org.gbif.api.service.directory.ParticipantService;
import org.gbif.registry.directorymock.mapper.RegistryIdentifierMockMapper;
import org.gbif.registry.directorymock.mapper.RegistryNodeMockMapper;
import org.gbif.registry.directorymock.service.NodeServiceMock;
import org.gbif.registry.directorymock.service.ParticipantServiceMock;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.NodeMapper;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;

/**
 * Provider that exposes generated test Injector.
 */
public class DirectoryUpdateTestProvider {

  private static Injector injector;

  public static Injector getMockInjector() {
    if( injector == null) {
      injector = Guice.createInjector(new DirectoryMockModule(), new RegistryMyBatisMockModule());
    }
    return injector;
  }

  /**
   * Mock module implementation to bind classes related to the Registry
   */
  private static class RegistryMyBatisMockModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(NodeMapper.class).to(RegistryNodeMockMapper.class).in(Scopes.SINGLETON);
      bind(IdentifierMapper.class).to(RegistryIdentifierMockMapper.class).in(Scopes.SINGLETON);
    }
  }

  /**
   * Mock module implementation to bind classes related to the Directory
   */
  private static class DirectoryMockModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(NodeService.class).to(NodeServiceMock.class).in(Scopes.SINGLETON);
      bind(ParticipantService.class).to(ParticipantServiceMock.class).in(Scopes.SINGLETON);
    }
  }

}
