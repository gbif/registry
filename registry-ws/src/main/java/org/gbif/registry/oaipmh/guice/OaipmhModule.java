package org.gbif.registry.oaipmh.guice;

import org.gbif.registry.oaipmh.OaipmhItemRepository;
import org.gbif.registry.oaipmh.OaipmhSetRepository;

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
 * A guice module that sets up implementation of XOAI related classes.
 *
 * @author cgendreau
 */
public class OaipmhModule extends AbstractModule {

  // earliest date a dataset was created
  public static final Date EARLIEST_DATE;
  static{
    Calendar cal = Calendar.getInstance();
    cal.set(2007,1,1);
    EARLIEST_DATE = cal.getTime();
  }

  public static final String OAI_PMH_PATH = "oaipmh";

  private String apiUrl;

  /**
   *
   * @param apiUrl api root url
   */
  public OaipmhModule(String apiUrl){
    this.apiUrl = apiUrl;
  }

  @Override
  protected void configure() {

    RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration()
            .withRepositoryName("GBIF Registry")
            .withAdminEmail("admin@gbif.org")
            .withBaseUrl(StringUtils.appendIfMissing(apiUrl,"/") + OAI_PMH_PATH)
            .withEarliestDate(EARLIEST_DATE)
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
