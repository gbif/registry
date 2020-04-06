/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.oaipmh;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.test.TestDataFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.UUID;

import org.dspace.xoai.model.oaipmh.MetadataFormat;
import org.dspace.xoai.serviceprovider.ServiceProvider;
import org.dspace.xoai.serviceprovider.client.HttpOAIClient;
import org.dspace.xoai.serviceprovider.client.OAIClient;
import org.dspace.xoai.serviceprovider.model.Context;
import org.junit.Rule;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;

/** Tests for the OaipmhEndpoint implementation. */
@RunWith(Parameterized.class)
public abstract class AbstractOaipmhEndpointIT {

  private final TestDataFactory testDataFactory;

  // used by OAIClient to access the OAI-PMH web service locally
  private String BASE_URL_FORMAT = "http://localhost:%d/oai-pmh/registry";

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

  @RegisterExtension
  static PreparedDbExtension database =
      EmbeddedPostgresExtension.preparedDatabase(
          LiquibasePreparer.forClasspathLocation("liquibase/master.xml"));

  @Rule
  public final DatabaseInitializer databaseRule =
      new DatabaseInitializer(database.getTestDatabase());

  private final NodeService nodeService;
  private final OrganizationService organizationService;
  private final InstallationService installationService;
  private final DatasetService datasetService;

  protected final String baseUrl;
  final ServiceProvider serviceProvider;

  @Autowired
  public AbstractOaipmhEndpointIT(
      NodeService nodeService,
      OrganizationService organizationService,
      InstallationService installationService,
      DatasetService datasetService,
      TestDataFactory testDataFactory) {
    this.nodeService = nodeService;
    this.organizationService = organizationService;
    this.installationService = installationService;
    this.datasetService = datasetService;
    this.testDataFactory = testDataFactory;

    baseUrl = String.format(BASE_URL_FORMAT, 0); // registryServer.getPort() TODO
    OAIClient oaiClient = new HttpOAIClient(baseUrl);
    Context context =
        new Context()
            .withOAIClient(oaiClient)
            .withMetadataTransformer(
                EML_FORMAT.getMetadataPrefix(),
                org.dspace.xoai.dataprovider.model.MetadataFormat.identity());
    serviceProvider = new ServiceProvider(context);
  }

  /**
   * Creates an Organization in the test database.
   *
   * @param publishingCountry
   * @return
   */
  Organization createOrganization(Country publishingCountry) {
    // endorsing node for the organization
    UUID nodeKey = nodeService.create(testDataFactory.newNode());
    // publishing organization (required field)
    Organization o = testDataFactory.newOrganization(nodeKey);
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
    Installation i = testDataFactory.newInstallation(organizationKey);
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
  Dataset createDataset(
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

  /**
   * Get the specified Dataset
   *
   * @param datasetKey
   * @return
   * @throws Exception
   */
  Dataset getDataset(UUID datasetKey) throws Exception {
    return datasetService.get(datasetKey);
  }

  /**
   * Delete the specified Dataset.
   *
   * @param datasetKey
   * @throws Exception
   */
  void deleteDataset(UUID datasetKey) throws Exception {
    datasetService.delete(datasetKey);
  }

  /**
   * Update the provided Dataset.
   *
   * @param dataset
   * @throws Exception
   */
  void updateDataset(Dataset dataset) throws Exception {
    datasetService.update(dataset);
  }

  /**
   * Insert metadata associated to the provided Dataset.
   *
   * @param key dataset key
   * @param document
   * @throws Exception
   */
  Metadata insertMetadata(UUID key, InputStream document) throws Exception {
    return datasetService.insertMetadata(key, document);
  }

  /**
   * This method is used to change the modified date of a dataset in order to test date queries.
   *
   * @param key
   * @param modifiedDate new modified date to set
   */
  void changeDatasetModifiedDate(UUID key, Date modifiedDate) throws Exception {
    Connection connection = null;
    try {
      connection = database.getTestDatabase().getConnection();
      connection.setAutoCommit(false);

      PreparedStatement p =
          connection.prepareStatement("UPDATE dataset SET modified = ? WHERE key = ?");

      p.setDate(1, new java.sql.Date(modifiedDate.getTime()));
      p.setObject(2, key);

      p.execute();
      connection.commit();
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }
}
