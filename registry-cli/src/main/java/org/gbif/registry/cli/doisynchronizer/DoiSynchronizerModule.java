package org.gbif.registry.cli.doisynchronizer;

import org.gbif.api.model.common.DOI;
import org.gbif.common.messaging.guice.PostalServiceModule;
import org.gbif.identity.guice.IdentityAccessModule;
import org.gbif.occurrence.query.TitleLookupModule;
import org.gbif.registry.doi.DoiModule;
import org.gbif.registry.persistence.guice.RegistryMyBatisModule;

import java.util.Properties;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 *
 */
public class DoiSynchronizerModule {

  private final DoiSynchronizerConfiguration config;

  public DoiSynchronizerModule(DoiSynchronizerConfiguration config){
    this.config = config;
  }

  public Injector getInjector(){
    return Guice.createInjector(
            new RegistryMyBatisModule(config.registry.toRegistryProperties()),
            new IdentityAccessModule(config.registry.toRegistryProperties()),
            new InnerRegistryModule()
    );
  }

  /**
   * Guice module for Registry related classes (except Mappers)
   */
  private class InnerRegistryModule extends AbstractModule {
    @Override
    protected void configure() {
      Properties prop = new Properties();
      prop.put("doi.prefix", DOI.GBIF_PREFIX);
      prop.put("portal.url", config.portalurl);
      install(new DoiModule(prop));
      install(new PostalServiceModule(PostalServiceConfiguration.SYNC_PREFIX, config.postalservice.toProperties()));
      install(new TitleLookupModule(true, config.apiRoot));

      //bind(DatasetService.class).to(DatasetResource.class);
    }
  }
}
