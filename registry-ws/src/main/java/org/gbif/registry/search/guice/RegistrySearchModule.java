package org.gbif.registry.search.guice;

import org.gbif.api.service.registry.DatasetSearchService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.common.search.solr.SolrConfig;
import org.gbif.common.search.solr.SolrModule;
import org.gbif.registry.search.util.DatasetIndexBuilder;
import org.gbif.registry.search.DatasetIndexService;
import org.gbif.registry.search.DatasetIndexUpdateListener;
import org.gbif.registry.search.DatasetSearchServiceImpl;
import org.gbif.registry.ws.resources.DatasetResource;
import org.gbif.registry.ws.resources.InstallationResource;
import org.gbif.registry.ws.resources.NodeResource;
import org.gbif.registry.ws.resources.OrganizationResource;

import java.util.Properties;

import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import org.apache.solr.client.solrj.SolrClient;

/**
 * A guice module that sets up implementation of the search services and gives SOLR providers for search.
 * It binds the solr client by name to support multiple solr clients, e.g. dataset and publisher searches.
 */
public class RegistrySearchModule extends PrivateModule {

  public static final String DATASET_BINDING_NAME = "dataset";
  public static final Key<SolrClient> DATASET_KEY = Key.get(SolrClient.class, Names.named(DATASET_BINDING_NAME));

  private static final String SOLR_DATASET_PREFIX = "solr.dataset.";
  private static final String SOLR_PUBLISHER_PREFIX = "solr.publisher.";
  private final Properties properties;

  public RegistrySearchModule(Properties properties) {
    this.properties = properties;
  }

  @Override
  public void configure() {
    // configure dataset solr
    SolrConfig datasetCfg = SolrConfig.fromProperties(properties, SOLR_DATASET_PREFIX);
    install(new SolrModule(datasetCfg, DATASET_BINDING_NAME));

    bind(DatasetService.class).to(DatasetResource.class).in(Scopes.SINGLETON);
    bind(OrganizationService.class).to(OrganizationResource.class).in(Scopes.SINGLETON);
    bind(NodeService.class).to(NodeResource.class).in(Scopes.SINGLETON);
    bind(InstallationService.class).to(InstallationResource.class).in(Scopes.SINGLETON);

    bind(DatasetSearchService.class).to(DatasetSearchServiceImpl.class).in(Scopes.SINGLETON);
    bind(DatasetIndexUpdateListener.class).asEagerSingleton();
    bind(DatasetIndexBuilder.class).in(Scopes.SINGLETON);
    bind(DatasetIndexService.class).in(Scopes.SINGLETON);

    expose(OrganizationService.class); // for testing
    expose(InstallationService.class); // for testing
    expose(DatasetService.class); // for testing
    expose(NodeService.class); // for testing
    expose(DatasetSearchService.class);
    expose(DatasetIndexUpdateListener.class); // for testing
    expose(DATASET_KEY); // for testing
    expose(DatasetIndexBuilder.class);
    expose(DatasetIndexService.class);


    // 6 email properties:
    // use dev email?
    bindBool("mail.devemail.enabled","useDevEmail");
    // smpt host?
    bindString("mail.smtp.host","smptHost");
    // smpt port?
    bindInt("mail.smtp.port","smptPort");
    // dev email address?
    bindString("mail.devemail","devEmail");
    // cc (helpdesk) email address?
    bindString("mail.cc","ccEmail");
    // from email address?
    bindString("mail.from","fromEmail");
  }

  /**
   * Map the String property "fromProperty" to and String named annotation "name".
   */
  private void bindString(String fromProperty , String name){
    if (properties.containsKey(fromProperty)) {
      String from = properties.getProperty(fromProperty);
      bind(String.class).annotatedWith(Names.named(name)).toInstance(from);
      expose(String.class).annotatedWith(Names.named(name));
    }
  }

  /**
   * Map the String property "fromProperty" to and Boolean named annotation "name".
   */
  private void bindBool(String fromProperty , String name){
    if (properties.containsKey(fromProperty)) {
      boolean from = Boolean.valueOf(properties.getProperty(fromProperty));
      bind(Boolean.class).annotatedWith(Names.named(name)).toInstance(from);
      expose(Boolean.class).annotatedWith(Names.named(name));
    }
  }

  /**
   * Map the String property "fromProperty" to and Integer named annotation "name".
   */
  private void bindInt(String fromProperty , String name){
    if (properties.containsKey(fromProperty)) {
      int from = Integer.valueOf(properties.getProperty(fromProperty));
      bind(Integer.class).annotatedWith(Names.named(name)).toInstance(from);
      expose(Integer.class).annotatedWith(Names.named(name));
    }
  }

}
