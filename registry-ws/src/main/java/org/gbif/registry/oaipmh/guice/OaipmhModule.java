package org.gbif.registry.oaipmh.guice;

import org.gbif.registry.oaipmh.OaipmhItemRepository;
import org.gbif.registry.oaipmh.OaipmhSetRepository;

import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import org.dspace.xoai.dataprovider.repository.ItemRepository;
import org.dspace.xoai.dataprovider.repository.RepositoryConfiguration;
import org.dspace.xoai.dataprovider.repository.SetRepository;
import org.dspace.xoai.model.oaipmh.DeletedRecord;
import org.dspace.xoai.model.oaipmh.Granularity;

/**
 * A guice module that binds implementations of XOAI related classes.
 *
 * @author cgendreau
 */
public class OaipmhModule extends AbstractModule {

  // earliest date a dataset was created
  public static final Date EARLIEST_DATE;
  static{
    Calendar cal = Calendar.getInstance();
    cal.setTimeZone(TimeZone.getTimeZone("UTC"));
    cal.set(2007,0,1,0,0,1);
    EARLIEST_DATE = cal.getTime();
  }

  public static final String OAIPMH_BASE_URL_PROPERTY = "oaipmh.baseUrl";
  public static final String OAIPMH_ADMIN_EMAIL_PROPERTY = "oaipmh.adminEmail";
  private static final String REPO_NAME = "GBIF Registry";

  private final RepositoryConfiguration repositoryConfiguration;

  /**
   *
   * @param properties OAI-PMH root url
   */
  public OaipmhModule(Properties properties){
    repositoryConfiguration = new RepositoryConfiguration()
            .withRepositoryName(REPO_NAME)
            .withAdminEmail(properties.getProperty(OAIPMH_ADMIN_EMAIL_PROPERTY))
            .withBaseUrl(properties.getProperty(OAIPMH_BASE_URL_PROPERTY))
            .withEarliestDate(EARLIEST_DATE)
            .withMaxListIdentifiers(1000)
            .withMaxListSets(1000)
            .withMaxListRecords(100)
            .withGranularity(Granularity.Second)
            .withDeleteMethod(DeletedRecord.PERSISTENT);
  }

  /**
   *
   * @param repositoryConfiguration an already configured RepositoryConfiguration object
   */
  public OaipmhModule(RepositoryConfiguration repositoryConfiguration){
    this.repositoryConfiguration = repositoryConfiguration;
  }

  @Override
  protected void configure() {
    bind(RepositoryConfiguration.class).toInstance(repositoryConfiguration);
    bind(ItemRepository.class).to(OaipmhItemRepository.class).in(Scopes.SINGLETON);
    bind(SetRepository.class).to(OaipmhSetRepository.class).in(Scopes.SINGLETON);
  }

}
