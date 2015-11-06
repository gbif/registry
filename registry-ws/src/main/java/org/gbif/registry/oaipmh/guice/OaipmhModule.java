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
 * A Guice module that binds implementations of XOAI related classes.
 *
 * DatasetResource, DatasetMapper, OrganizationMapper, CubeService must be bind externally.
 *
 */
public class OaipmhModule extends AbstractModule {

  // earliest date a dataset was created
  public static final Date EARLIEST_DATE;
  static {
    Calendar cal = Calendar.getInstance();
    cal.setTimeZone(TimeZone.getTimeZone("UTC"));
    cal.set(2007, 0, 1, 0, 0, 1);
    EARLIEST_DATE = cal.getTime();
  }

  public static final String OAIPMH_BASE_URL_PROPERTY = "oaipmh.baseUrl";
  public static final String OAIPMH_ADMIN_EMAIL_PROPERTY = "oaipmh.adminEmail";

  private static final String REPO_NAME = "GBIF Registry";

  private final RepositoryConfiguration repositoryConfiguration;

  /**
   * @param properties OAI-PMH root url
   */
  public OaipmhModule(Properties properties) {
    this(new RepositoryConfiguration()
      .withRepositoryName(REPO_NAME)
      .withAdminEmail(properties.getProperty(OAIPMH_ADMIN_EMAIL_PROPERTY))
      .withBaseUrl(properties.getProperty(OAIPMH_BASE_URL_PROPERTY))
      .withEarliestDate(EARLIEST_DATE)
      .withMaxListIdentifiers(1000)
      .withMaxListSets(1000)
      .withMaxListRecords(100)
      .withGranularity(Granularity.Second)
      .withDeleteMethod(DeletedRecord.PERSISTENT)
      .withDescription(
              "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" +
                      "\t<dc:title>GBIF Registry</dc:title>\n" +
                      "\t<dc:creator>Global Biodiversity Information Facility Secretariat</dc:creator>\n" +
                      "\t<dc:description>\n" +
                      "\t\tThe GBIF Registry — the entities that make up the GBIF network.\n" +
                      "\t\tThis OAI-PMH service exposes Datasets, organized into sets of country, installation and resource type.\n" +
                      "\t\tFor more information, see http://www.gbif.org/developer/registry\n" +
                      "\t</dc:description>\n" +
                      "</oai_dc:dc>\n"
      ));
  }

  /**
   *
   * @param repositoryConfiguration an already configured RepositoryConfiguration object
   */
  public OaipmhModule(RepositoryConfiguration repositoryConfiguration) {
    this.repositoryConfiguration = repositoryConfiguration;
  }

  @Override
  protected void configure() {
    bind(RepositoryConfiguration.class).toInstance(repositoryConfiguration);
    bind(ItemRepository.class).to(OaipmhItemRepository.class).in(Scopes.SINGLETON);
    bind(SetRepository.class).to(OaipmhSetRepository.class).in(Scopes.SINGLETON);
  }

}
