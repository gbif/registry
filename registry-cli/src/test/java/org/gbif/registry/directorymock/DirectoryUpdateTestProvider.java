package org.gbif.registry.directorymock;

import org.gbif.api.service.directory.NodeService;
import org.gbif.api.service.directory.ParticipantService;
import org.gbif.registry.cli.directoryupdate.DirectoryUpdateConfiguration;
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Provider that exposes generated test Injector.
 */
public class DirectoryUpdateTestProvider {

  private static Injector injClient;
  private static Injector injMyBatis;

  public static DirectoryUpdateConfiguration getMockDirectoryUpdateConfiguration(){

    DirectoryUpdateConfiguration directoryUpdateConfiguration = mock(DirectoryUpdateConfiguration.class);
    when(directoryUpdateConfiguration.createInjector()).thenReturn(getMockInjector());
    when(directoryUpdateConfiguration.createMyBatisInjector()).thenReturn(getMyBatisInjector());

    return directoryUpdateConfiguration;
  }

  public static Injector getMockInjector() {
    if( injClient == null) {
      injClient = Guice.createInjector(new DirectoryMockModule());
    }
    return injClient;
  }

  public static Injector getMyBatisInjector(){
    if( injMyBatis == null ){
      injMyBatis = Guice.createInjector(new RegistryMyBatisMockModule());
    }
    return injMyBatis;
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
