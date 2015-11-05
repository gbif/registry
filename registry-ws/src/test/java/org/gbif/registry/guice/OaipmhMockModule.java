package org.gbif.registry.guice;

import org.gbif.api.model.metrics.cube.ReadBuilder;
import org.gbif.api.model.metrics.cube.Rollup;
import org.gbif.api.service.metrics.CubeService;
import org.gbif.registry.oaipmh.OaipmhItemRepository;
import org.gbif.registry.oaipmh.OaipmhSetRepository;

import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import org.dspace.xoai.dataprovider.repository.ItemRepository;
import org.dspace.xoai.dataprovider.repository.RepositoryConfiguration;
import org.dspace.xoai.dataprovider.repository.SetRepository;

/**
 * Mock class that is mainly used to mock the CubeService.
 */
public class OaipmhMockModule extends AbstractModule {

  private RepositoryConfiguration repositoryConfiguration;
  public OaipmhMockModule(RepositoryConfiguration repositoryConfiguration){
    this.repositoryConfiguration = repositoryConfiguration;
  }

  @Override
  protected void configure() {
    bind(RepositoryConfiguration.class).toInstance(repositoryConfiguration);
    bind(ItemRepository.class).to(OaipmhItemRepository.class).in(Scopes.SINGLETON);
    bind(SetRepository.class).to(OaipmhSetRepository.class).in(Scopes.SINGLETON);
    bind(CubeService.class).to(MockCubeService.class).in(Scopes.SINGLETON);
  }

  private static class MockCubeService implements CubeService {

    @Override
    public long get(ReadBuilder readBuilder) throws IllegalArgumentException {
      return 0;
    }

    @Override
    public List<Rollup> getSchema() {
      return null;
    }
  }

}
