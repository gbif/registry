package org.gbif.registry.oaipmh.guice;

import org.gbif.registry.oaipmh.OaipmhItemRepository;
import org.gbif.registry.oaipmh.OaipmhSetRepository;

import java.util.Date;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import org.dspace.xoai.dataprovider.repository.ItemRepository;
import org.dspace.xoai.dataprovider.repository.RepositoryConfiguration;
import org.dspace.xoai.dataprovider.repository.SetRepository;
import org.dspace.xoai.model.oaipmh.DeletedRecord;
import org.dspace.xoai.model.oaipmh.Granularity;

/**
 * A guice module that sets up implementation of XOAI related classes.
 *
 * @author cgendreau
 */
public class OaipmhModule extends AbstractModule {

  private String baseUrl;

  public OaipmhModule(String baseUrl){
    this.baseUrl = baseUrl;
  }

  @Override
  protected void configure() {

    RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration()
            .withRepositoryName("GBIF Registry")
            .withAdminEmail("admin@gbif.org")
            .withBaseUrl(baseUrl)
            .withEarliestDate(new Date())
            .withMaxListIdentifiers(1000)
            .withMaxListSets(1000)
            .withMaxListRecords(100)
            .withGranularity(Granularity.Second)
            .withDeleteMethod(DeletedRecord.PERSISTENT)
            .withDescription("<TestDescription xmlns=\"\">Test description</TestDescription>");

    bind(RepositoryConfiguration.class).toInstance(repositoryConfiguration);
    bind(ItemRepository.class).to(OaipmhItemRepository.class).in(Scopes.SINGLETON);
    bind(SetRepository.class).to(OaipmhSetRepository.class).in(Scopes.SINGLETON);
  }

}
