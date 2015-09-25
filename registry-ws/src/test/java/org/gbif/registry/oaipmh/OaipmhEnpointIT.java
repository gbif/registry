package org.gbif.registry.oaipmh;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.utils.Datasets;
import org.gbif.registry.utils.Installations;
import org.gbif.registry.utils.Nodes;
import org.gbif.registry.utils.Organizations;

import java.sql.Connection;
import java.util.Date;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.gbif.registry.guice.RegistryTestModules.webservice;
import static org.gbif.registry.guice.RegistryTestModules.webserviceClient;

import java.sql.PreparedStatement;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for the OaipmhEndpoint implementation.
 */
@RunWith(Parameterized.class)
public class OaipmhEnpointIT {

  // Flushes the database on each run
  @ClassRule
  public static final LiquibaseInitializer liquibaseRule = new LiquibaseInitializer(RegistryTestModules.database());

  @Rule
  public final DatabaseInitializer databaseRule = new DatabaseInitializer(RegistryTestModules.database());

  private final NodeService nodeService;
  private final OrganizationService organizationService;
  private final InstallationService installationService;
  private final DatasetService datasetService;

  public OaipmhEnpointIT(NodeService nodeService, OrganizationService organizationService, InstallationService installationService,
                         DatasetService datasetService){
    this.nodeService = nodeService;
    this.organizationService = organizationService;
    this.installationService = installationService;
    this.datasetService = datasetService;
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
   * TODO incomplete test
   * @throws Throwable
   */
  @Test
  public void prepareData() throws Throwable {
    Organization org1 = createOrganization(Country.ICELAND);
    Installation org1Installation1 = createInstallation(org1.getKey());
    Dataset org1Installation1Dataset1 = createDataset(org1.getKey(), org1Installation1.getKey(), DatasetType.CHECKLIST, new Date());

    Installation org1Installation2 = createInstallation(org1.getKey());
    Dataset org1Installation2Dataset1 = createDataset(org1.getKey(), org1Installation2.getKey(), DatasetType.OCCURRENCE, new Date());

    Organization org2 = createOrganization(Country.NEW_ZEALAND);
    Installation org2Installation1 = createInstallation(org2.getKey());
    Dataset org2Installation1Dataset1 = createDataset(org2.getKey(), org2Installation1.getKey(), DatasetType.CHECKLIST, new Date());

    PagingResponse<Dataset> datasetList =  datasetService.listByCountry(Country.ICELAND, null, null);
    assertEquals(2, datasetList.getResults().size());

    datasetList =  datasetService.listByCountry(Country.NEW_ZEALAND, null, null);
    assertEquals(1, datasetList.getResults().size());

  }

  /**
   * Creates an Organization in the test database.
   *
   * @param publishingCountry
   * @return
   */
  private Organization createOrganization(Country publishingCountry){
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
  private Installation createInstallation(UUID organizationKey){
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
  private Dataset createDataset(UUID organizationKey, UUID installationKey, DatasetType type, Date modifiedDate) throws Throwable {

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
  private void changeDatasetModifiedDate(UUID key, Date modifiedDate) throws Throwable {
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
}
