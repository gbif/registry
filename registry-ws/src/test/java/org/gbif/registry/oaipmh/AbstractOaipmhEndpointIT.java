package org.gbif.registry.oaipmh;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.utils.Datasets;
import org.gbif.registry.utils.Installations;
import org.gbif.registry.utils.Nodes;
import org.gbif.registry.utils.Organizations;
import org.gbif.registry.ws.resources.OccurrenceDownloadResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import org.dspace.xoai.model.oaipmh.MetadataFormat;
import org.dspace.xoai.serviceprovider.ServiceProvider;
import org.dspace.xoai.serviceprovider.client.HttpOAIClient;
import org.dspace.xoai.serviceprovider.client.OAIClient;
import org.dspace.xoai.serviceprovider.model.Context;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.gbif.registry.guice.RegistryTestModules.webservice;
import static org.gbif.registry.guice.RegistryTestModules.webserviceClient;

import java.sql.PreparedStatement;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;

import static org.junit.Assert.assertEquals;

/**
 * Tests for the OaipmhEndpoint implementation.
 */
@RunWith(Parameterized.class)
public abstract class AbstractOaipmhEndpointIT {

  private String BASE_URL_FORMAT = "http://localhost:%d/oaipmh";

  protected MetadataFormat OAIDC_FORMAT = new MetadataFormat()
          .withMetadataPrefix("oai_dc")
          .withMetadataNamespace("http://www.openarchives.org/OAI/2.0/oai_dc/")
          .withSchema("http://www.openarchives.org/OAI/2.0/oai_dc.xsd");

  protected MetadataFormat EML_FORMAT = new MetadataFormat()
          .withMetadataPrefix("eml")
          .withMetadataNamespace("eml://ecoinformatics.org/eml-2.1.1")
          .withSchema("http://rs.gbif.org/schema/eml-gbif-profile/1.0.2/eml.xsd");

  // Flushes the database on each run
  @ClassRule
  public static final LiquibaseInitializer liquibaseRule = new LiquibaseInitializer(RegistryTestModules.database());

  @ClassRule
  public static final RegistryServer registryServer = RegistryServer.INSTANCE;

  @Rule
  public final DatabaseInitializer databaseRule = new DatabaseInitializer(RegistryTestModules.database());

  private final NodeService nodeService;
  private final OrganizationService organizationService;
  private final InstallationService installationService;
  private final DatasetService datasetService;

  final String baseUrl;
  final ServiceProvider serviceProvider;

  public AbstractOaipmhEndpointIT(NodeService nodeService, OrganizationService organizationService, InstallationService installationService,
                                  DatasetService datasetService){
    this.nodeService = nodeService;
    this.organizationService = organizationService;
    this.installationService = installationService;
    this.datasetService = datasetService;

    baseUrl = String.format(BASE_URL_FORMAT, registryServer.getPort());
    OAIClient oaiClient = new HttpOAIClient(baseUrl);
    Context context = new Context().withOAIClient(oaiClient).withMetadataTransformer(EML_FORMAT.getMetadataPrefix(), org.dspace.xoai.dataprovider.model.MetadataFormat.identity());
    serviceProvider = new ServiceProvider(context);
  }

  @Parameters
  public static Iterable<Object[]> data() {
    final Injector webservice = webservice();
    //final Injector client = webserviceClient();
    return ImmutableList.<Object[]>of(new Object[]{
            webservice.getInstance(NodeService.class),
            webservice.getInstance(OrganizationService.class),
            webservice.getInstance(InstallationService.class),
            webservice.getInstance(DatasetService.class)});
  }

  /**
   * Creates an Organization in the test database.
   *
   * @param publishingCountry
   * @return
   */
  Organization createOrganization(Country publishingCountry) {
    // endorsing node for the organization
    UUID nodeKey = nodeService.create(Nodes.newInstance());
    // publishing organization (required field)
    Organization o = Organizations.newInstance(nodeKey);
    o.setCountry(publishingCountry);
    organizationService.create(o);
    return o;
  }

  /**
   * Creates an Installation in the test database.
   *
   * @param organizationKey
   * @return
   */
  Installation createInstallation(UUID organizationKey) {
    Installation i = Installations.newInstance(organizationKey);
    installationService.create(i);
    return i;
  }

  /**
   * Creates a Dataset in the test database.
   *
   * @param organizationKey
   * @param installationKey
   * @param type
   * @param modifiedDate
   * @return the newly created Dataset
   * @throws Throwable
   */
  Dataset createDataset(UUID organizationKey, UUID installationKey, DatasetType type, Date modifiedDate) throws Exception {

    Dataset d = Datasets.newInstance(organizationKey, installationKey);
    d.setType(type);
    datasetService.create(d);

    // since modifiedDate is set automatically by the datasetMapper we update it manually
    changeDatasetModifiedDate(d.getKey(), modifiedDate);
    d.setModified(modifiedDate);

    return d;
  }

  /**
   * This method is used to change the modified date of a dataset in order to test date queries.
   *
   * @param key
   * @param modifiedDate new modified date to set
   *
   */
  void changeDatasetModifiedDate(UUID key, Date modifiedDate) throws Exception {
    Connection connection = null;
    try {
      connection = RegistryTestModules.database().getConnection();
      connection.setAutoCommit(false);

      PreparedStatement p = connection.prepareStatement("UPDATE dataset SET modified = ? WHERE key = ?");

      p.setDate(1, new java.sql.Date(modifiedDate.getTime()));
      p.setObject(2, key);

      p.execute();
      connection.commit();
    }finally {
      if (connection != null) {
        connection.close();
      }
    }
  }

  /**
   * Same as {@link org.hamcrest.collection.IsIterableWithSize} but for Iterator since XOAI library returns iterators directly.
   *
   * Usage: assertThat(records, IsIterorWithSize.<Record>iteratorWithSize(3))
   *
   * @param <E>
   */
  static class IsIterorWithSize<E> extends FeatureMatcher<Iterator<E>, Integer> {

    public IsIterorWithSize(org.hamcrest.Matcher<? super Integer> sizeMatcher) {
      super(sizeMatcher, "an iterator with size", "iterator size");
    }

    @Override
    protected Integer featureValueOf(Iterator<E> iterator) {
      int size = 0;

      while(iterator.hasNext()) {
        ++size;
        iterator.next();
      }
      return Integer.valueOf(size);
    }

    public static <E> org.hamcrest.Matcher<Iterator<E>> iteratorWithSize(org.hamcrest.Matcher<? super Integer> sizeMatcher) {
      return new IsIterorWithSize(sizeMatcher);
    }

    public static <E> org.hamcrest.Matcher<Iterator<E>> iteratorWithSize(int size) {
      return iteratorWithSize(IsEqual.equalTo(Integer.valueOf(size)));
    }
  }
}
