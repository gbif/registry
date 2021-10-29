/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.ws.it.oaipmh;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.UUID;

import javax.sql.DataSource;

import org.dspace.xoai.model.oaipmh.MetadataFormat;
import org.dspace.xoai.serviceprovider.ServiceProvider;
import org.dspace.xoai.serviceprovider.client.HttpOAIClient;
import org.dspace.xoai.serviceprovider.client.OAIClient;
import org.dspace.xoai.serviceprovider.model.Context;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/** Tests for the OaipmhEndpoint implementation. */
public abstract class AbstractOaipmhEndpointIT extends BaseItTest {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer();

  private final TestDataFactory testDataFactory;

  protected MetadataFormat OAIDC_FORMAT =
      new MetadataFormat()
          .withMetadataPrefix("oai_dc")
          .withMetadataNamespace("http://www.openarchives.org/OAI/2.0/oai_dc/")
          .withSchema("http://www.openarchives.org/OAI/2.0/oai_dc.xsd");

  protected MetadataFormat EML_FORMAT =
      new MetadataFormat()
          .withMetadataPrefix("eml")
          .withMetadataNamespace("eml://ecoinformatics.org/eml-2.1.1")
          .withSchema("http://rs.gbif.org/schema/eml-gbif-profile/1.0.2/eml.xsd");

  private final NodeService nodeService;
  private final OrganizationService organizationService;
  private final InstallationService installationService;
  private final DatasetService datasetService;
  private final DataSource dataSource;

  protected final String baseUrl;
  protected final ServiceProvider serviceProvider;

  @Autowired
  public AbstractOaipmhEndpointIT(
      SimplePrincipalProvider principalProvider,
      Environment environment,
      NodeService nodeService,
      OrganizationService organizationService,
      InstallationService installationService,
      DatasetService datasetService,
      TestDataFactory testDataFactory,
      EsManageServer esServer,
      DataSource dataSource) {
    super(principalProvider, esServer);
    this.nodeService = nodeService;
    this.organizationService = organizationService;
    this.installationService = installationService;
    this.datasetService = datasetService;
    this.testDataFactory = testDataFactory;
    this.dataSource = dataSource;

    String port = environment.getProperty("local.server.port");
    baseUrl = String.format("http://localhost:%s/oai-pmh/registry", port);
    OAIClient oaiClient = new HttpOAIClient(baseUrl);
    Context context =
        new Context()
            .withOAIClient(oaiClient)
            .withMetadataTransformer(
                EML_FORMAT.getMetadataPrefix(),
                org.dspace.xoai.dataprovider.model.MetadataFormat.identity());
    serviceProvider = new ServiceProvider(context);
  }

  /** Creates an Organization in the test database. */
  protected Organization createOrganization(Country publishingCountry) {
    // endorsing node for the organization
    UUID nodeKey = nodeService.create(testDataFactory.newNode());
    // publishing organization (required field)
    Organization o = testDataFactory.newOrganization(nodeKey);
    o.setCountry(publishingCountry);
    organizationService.create(o);
    return o;
  }

  /** Creates an Installation in the test database. */
  protected Installation createInstallation(UUID organizationKey) {
    Installation i = testDataFactory.newInstallation(organizationKey);
    installationService.create(i);
    return i;
  }

  /** Creates a Dataset in the test database. */
  protected Dataset createDataset(
      UUID organizationKey, UUID installationKey, DatasetType type, Date modifiedDate)
      throws Exception {

    Dataset d = testDataFactory.newDataset(organizationKey, installationKey);
    d.setType(type);
    datasetService.create(d);

    // since modifiedDate is set automatically by the datasetMapper we update it manually
    changeDatasetModifiedDate(d.getKey(), modifiedDate);
    d.setModified(modifiedDate);

    return d;
  }

  /** Get the specified Dataset */
  protected Dataset getDataset(UUID datasetKey) {
    return datasetService.get(datasetKey);
  }

  /** Delete the specified Dataset. */
  protected void deleteDataset(UUID datasetKey) {
    datasetService.delete(datasetKey);
  }

  /** Update the provided Dataset. */
  protected void updateDataset(Dataset dataset) {
    datasetService.update(dataset);
  }

  /** Insert metadata associated to the provided Dataset. */
  protected void insertMetadata(UUID key, InputStream document) {
    datasetService.insertMetadata(key, document);
  }

  /** This method is used to change the modified date of a dataset in order to test date queries. */
  protected void changeDatasetModifiedDate(UUID key, Date modifiedDate) throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);

      PreparedStatement p =
          connection.prepareStatement("UPDATE dataset SET modified = ? WHERE key = ?");

      p.setDate(1, new java.sql.Date(modifiedDate.getTime()));
      p.setObject(2, key);

      p.execute();
      connection.commit();
    }
  }
}
