package org.gbif.registry.guice;

import org.gbif.registry.oaipmh.OaipmhItemRepository;
import org.gbif.registry.oaipmh.OaipmhSetRepository;
import org.gbif.registry.oaipmh.guice.OaipmhModule;

import java.util.Calendar;
import java.util.Date;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import org.apache.commons.lang3.StringUtils;
import org.dspace.xoai.dataprovider.repository.ItemRepository;
import org.dspace.xoai.dataprovider.repository.RepositoryConfiguration;
import org.dspace.xoai.dataprovider.repository.SetRepository;
import org.dspace.xoai.model.oaipmh.DeletedRecord;
import org.dspace.xoai.model.oaipmh.Granularity;

/**
 *
 * Mock Oaipmh Guice module for testing.
 * All list requests maximum are set to 2 (MaxListIdentifiers, MaxListSets, MaxListRecords) to test paging.
 *
 * @author cgendreau
 */
public class OaipmhMockModule extends AbstractModule {

  public static final int MAX_LIST_RECORDS = 2;

  private String apiUrl;

  public OaipmhMockModule(String apiUrl){
    this.apiUrl = apiUrl;
  }

  @Override
  protected void configure() {

    RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration()
            .withRepositoryName("GBIF Test Registry")
            .withAdminEmail("admin@gbif.org")
            .withBaseUrl(StringUtils.appendIfMissing(apiUrl, "/") + OaipmhModule.OAI_PMH_PATH)
            .withEarliestDate(OaipmhModule.EARLIEST_DATE)
            .withMaxListIdentifiers(2)
            .withMaxListSets(2)
            .withMaxListRecords(MAX_LIST_RECORDS)
            .withGranularity(Granularity.Second)
            .withDeleteMethod(DeletedRecord.PERSISTENT)
            .withDescription("<TestDescription xmlns=\"\">Test description</TestDescription>");

    bind(RepositoryConfiguration.class).toInstance(repositoryConfiguration);
    bind(ItemRepository.class).to(OaipmhItemRepository.class).in(Scopes.SINGLETON);
    bind(SetRepository.class).to(OaipmhSetRepository.class).in(Scopes.SINGLETON);
  }
}