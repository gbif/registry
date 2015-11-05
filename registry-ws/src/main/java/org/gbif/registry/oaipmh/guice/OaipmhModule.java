package org.gbif.registry.oaipmh.guice;

import org.gbif.api.service.metrics.CubeService;
import org.gbif.metrics.ws.client.CubeWsClient;
import org.gbif.registry.oaipmh.OaipmhItemRepository;
import org.gbif.registry.oaipmh.OaipmhSetRepository;
import org.gbif.utils.HttpUtil;
import org.gbif.ws.json.JacksonJsonContextResolver;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import com.beust.jcommander.internal.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.apache4.ApacheHttpClient4;
import com.sun.jersey.client.apache4.ApacheHttpClient4Handler;
import org.apache.http.client.HttpClient;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.dspace.xoai.dataprovider.repository.ItemRepository;
import org.dspace.xoai.dataprovider.repository.RepositoryConfiguration;
import org.dspace.xoai.dataprovider.repository.SetRepository;
import org.dspace.xoai.model.oaipmh.DeletedRecord;
import org.dspace.xoai.model.oaipmh.Granularity;

/**
 * A Guice module that binds implementations of XOAI related classes.
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

  private static final String API_URL_PROPERTY = "api.url";
  private static final String REPO_NAME = "GBIF Registry";
  private static final int HTTPCLIENT_TIMEOUT_MS = 100;

  private final RepositoryConfiguration repositoryConfiguration;
  private final Properties properties;

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
      ), properties);
  }

  /**
   *
   * @param repositoryConfiguration an already configured RepositoryConfiguration object
   */
  public OaipmhModule(RepositoryConfiguration repositoryConfiguration, Properties properties) {
    this.repositoryConfiguration = repositoryConfiguration;
    this.properties = properties;
  }

  @Override
  protected void configure() {
    Map<String, String> prop = Maps.newHashMap();
    prop.put(API_URL_PROPERTY, properties.get(API_URL_PROPERTY).toString());
    Names.bindProperties(this.binder(), prop);

    bind(RepositoryConfiguration.class).toInstance(repositoryConfiguration);
    bind(ItemRepository.class).to(OaipmhItemRepository.class).in(Scopes.SINGLETON);
    bind(SetRepository.class).to(OaipmhSetRepository.class).in(Scopes.SINGLETON);
    install(new InternalCubeModule());
  }

  /**
   * Nested PrivateModule to not expose the Client and HttpClient.
   */
  private static final class InternalCubeModule extends PrivateModule {

    @Override
    protected void configure() {
      bind(CubeService.class).to(CubeWsClient.class);
      expose(CubeService.class);
    }

    @Provides
    @Singleton
    private WebResource provideBaseWsWebResource(Client client, @Named(API_URL_PROPERTY) String url) {
      return client.resource(url);
    }

    @Provides
    @Singleton
    @Inject
    public Client providesJerseyClient(HttpClient client) {
      return buildJerseyClient(client);
    }

    @Provides
    @Singleton
    private HttpClient provideHttpClient() {
      int maxHttpConnections = 100;
      int maxHttpConnectionsPerRoute = 100;
      return HttpUtil.newMultithreadedClient(HTTPCLIENT_TIMEOUT_MS, maxHttpConnections, maxHttpConnectionsPerRoute);
    }

    private static Client buildJerseyClient(HttpClient client) {
      ApacheHttpClient4Handler hch = new ApacheHttpClient4Handler(client, null, false);
      DefaultClientConfig clientConfig = new DefaultClientConfig();
      clientConfig.getClasses().add(JacksonJsonContextResolver.class);
      clientConfig.getClasses().add(JacksonJsonProvider.class);
      clientConfig.getFeatures().put("com.sun.jersey.api.json.POJOMappingFeature", Boolean.TRUE);
      return new ApacheHttpClient4(hch, clientConfig);
    }
  }

}
