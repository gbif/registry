package org.gbif.registry.search.guice;

import org.gbif.api.service.registry.DatasetSearchService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.common.search.inject.SolrModule;
import org.gbif.common.search.solr.builders.EmbeddedServerBuilder;
import org.gbif.registry.search.DatasetIndexBuilder;
import org.gbif.registry.search.DatasetIndexUpdateListener;
import org.gbif.registry.search.DatasetSearchServiceSolr;
import org.gbif.registry.ws.resources.DatasetResource;
import org.gbif.registry.ws.resources.InstallationResource;
import org.gbif.registry.ws.resources.NodeResource;
import org.gbif.registry.ws.resources.OrganizationResource;
import org.gbif.service.guice.PrivateServiceModule;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Properties;

import com.google.common.io.Resources;
import com.google.inject.Exposed;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;

/**
 * A guice module that sets up implementation of the search services and gives SOLR providers for search.
 * <p/>
 * The current implementation uses an embedded SOLR index, that uses a SOLR RAM directory. This is satisfactory while
 * indexes remain small, and while we opt to rebuild the index on startup. Future implementations might consider using
 * an external SOLR server, thus this method signatures use abstract SOLR type, and not a concrete implementation to
 * allow for future changes.
 * <p/>
 * <strong>A note on the gbif-common-search SolrModule {@link SolrModule}:</strong>
 * <p/>
 * The {@link SolrModule} was considered for use here, but discarded because in its current implementation:
 * <ul>
 * <li>The {@link EmbeddedServerBuilder} does not support multiple cores, but that is anticipated as a future
 * requirement for this project</li>
 * <li>The {@link EmbeddedServerBuilder} does not allow someone to supply additional configuration files (it was
 * developed for unit test purposes only)</li>
 * <li>The SOLR server desired requires only 3 lines of code to instantiate. The implementor felt that readability is
 * improved keeping this together</li>
 * <li>{@link SolrModule} is complex as it provides configuration based declaration (e.g. to support varying
 * deployments. The implementor felt that was an additional learning curve that is unnecessary to put on future
 * developers of this project. That said, should this project require flexible deployment options in the future,
 * developers would be wise to consider adding multi-core support to the {@link SolrModule}</li>
 * </ul>
 */
public class RegistrySearchModule extends PrivateServiceModule {

  private static final String REGISTRY_PROPERTY_PREFIX = "registry.search.";

  public RegistrySearchModule(Properties properties) {
    super(REGISTRY_PROPERTY_PREFIX, properties);
  }

  @Override
  public void configureService() {
    bind(DatasetSearchService.class).to(DatasetSearchServiceSolr.class).in(Scopes.SINGLETON);
    bind(DatasetService.class).to(DatasetResource.class).in(Scopes.SINGLETON);
    bind(OrganizationService.class).to(OrganizationResource.class).in(Scopes.SINGLETON);
    bind(NodeService.class).to(NodeResource.class).in(Scopes.SINGLETON);
    bind(InstallationService.class).to(InstallationResource.class).in(Scopes.SINGLETON);
    bind(DatasetIndexUpdateListener.class).asEagerSingleton();
    bind(DatasetIndexBuilder.class).in(Scopes.SINGLETON);
    expose(DatasetSearchService.class);
    expose(DatasetIndexUpdateListener.class); // for testing
    expose(OrganizationService.class); // for testing
    expose(InstallationService.class); // for testing
    expose(DatasetService.class); // for testing
    expose(NodeService.class); // for testing

    // boolean instance binding named sandboxmode.enabled, defaults to false if property is null
    boolean sandboxMode = Boolean.valueOf(getVerbatimProperties().getProperty("registry.sandboxmode.enabled"));
    bind(Boolean.class).annotatedWith(Names.named("sandboxmode.enabled")).toInstance(sandboxMode);
    expose(Boolean.class).annotatedWith(Names.named("sandboxmode.enabled"));

    // 6 email properties:
    // use dev email?
    boolean useDevEmail = Boolean.valueOf(getVerbatimProperties().getProperty("mail.devemail.enabled"));
    bind(Boolean.class).annotatedWith(Names.named("useDevEmail")).toInstance(useDevEmail);
    expose(Boolean.class).annotatedWith(Names.named("useDevEmail"));
    // smpt host?
    String smptHost = getVerbatimProperties().getProperty("mail.smtp.host");
    bind(String.class).annotatedWith(Names.named("smptHost")).toInstance(smptHost);
    expose(String.class).annotatedWith(Names.named("smptHost"));
    // smpt port?
    int smptPort = Integer.valueOf(getVerbatimProperties().getProperty("mail.smtp.port"));
    bind(Integer.class).annotatedWith(Names.named("smptPort")).toInstance(smptPort);
    expose(Integer.class).annotatedWith(Names.named("smptPort"));
    // dev email address?
    String devEmail = getVerbatimProperties().getProperty("mail.devemail");
    bind(String.class).annotatedWith(Names.named("devEmail")).toInstance(devEmail);
    expose(String.class).annotatedWith(Names.named("devEmail"));
    // cc (helpdesk) email address?
    String ccEmail = getVerbatimProperties().getProperty("mail.cc");
    bind(String.class).annotatedWith(Names.named("ccEmail")).toInstance(ccEmail);
    expose(String.class).annotatedWith(Names.named("ccEmail"));
    // from email address?
    String fromEmail = getVerbatimProperties().getProperty("mail.from");
    bind(String.class).annotatedWith(Names.named("fromEmail")).toInstance(fromEmail);
    expose(String.class).annotatedWith(Names.named("fromEmail"));
  }

  /**
   * A provider of the SolrServer to use with a dataset search.
   * Named as such, to allow multiple providers (e.g. solr cores) should this be required in the future.
   * 
   * @return An embedded solr server that uses the configuration in /solr on the classpath
   * @throws URISyntaxException If /solr configuration is not found on the classpath
   */
  @Provides
  @Exposed
  @Named("Dataset")
  @Singleton
  public SolrServer datasetSolr() throws URISyntaxException {
    File solrDir = new File(Resources.getResource("solr").toURI());
    File conf = new File(solrDir, "solr.xml");
    EmbeddedSolrServer solr = new EmbeddedSolrServer(CoreContainer.createAndLoad(solrDir.getAbsolutePath(), conf), "dataset");
    return solr;
  }
}
