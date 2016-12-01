package org.gbif.registry.cli.datasetupdater;

import org.gbif.api.service.registry.DatasetSearchService;
import org.gbif.registry.cli.common.stubs.DoiGeneratorStub;
import org.gbif.registry.cli.common.stubs.DoiHandlerStrategyStub;
import org.gbif.registry.cli.common.stubs.EditorAuthorizationServiceStub;
import org.gbif.registry.cli.common.stubs.SearchServiceStub;
import org.gbif.registry.doi.generator.DoiGenerator;
import org.gbif.registry.doi.handler.DataCiteDoiHandlerStrategy;
import org.gbif.registry.persistence.guice.RegistryMyBatisModule;
import org.gbif.registry.ws.security.EditorAuthorizationService;
import org.gbif.ws.client.guice.GbifApplicationAuthModule;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * A Guice module used exclusively by DatasetUpdater, to use DatasetResource directly.
 */
public class DatasetUpdaterModule {

  private final DatasetUpdaterConfiguration config;

  public DatasetUpdaterModule(DatasetUpdaterConfiguration config) {
    this.config = config;
  }

  public Injector getInjector() {
    return Guice.createInjector(new RegistryMyBatisModule(config.db.toRegistryProperties()), new InnerRegistryModule(),
      new GbifApplicationAuthModule(config.db.user, config.db.password));
  }

  /**
   * Guice module for Registry related classes (except Mappers).
   * To simplify binding, classes that are required but aren't used by CLI are stubbed.
   */
  private class InnerRegistryModule extends AbstractModule {

    @Override
    protected void configure() {
      bind(DatasetSearchService.class).to(SearchServiceStub.class);
      bind(DoiGenerator.class).to(DoiGeneratorStub.class);
      bind(DataCiteDoiHandlerStrategy.class).to(DoiHandlerStrategyStub.class);
      bind(EditorAuthorizationService.class).to(EditorAuthorizationServiceStub.class);
    }
  }
}
