/*
 * Copyright 2013 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry;


import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetOccurrenceDownloadUsageService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.utils.Datasets;
import org.gbif.registry.utils.Installations;
import org.gbif.registry.utils.Nodes;
import org.gbif.registry.utils.Organizations;
import org.gbif.registry.ws.resources.DatasetOccurrenceDownloadUsageResource;
import org.gbif.registry.ws.resources.DatasetResource;
import org.gbif.registry.ws.resources.InstallationResource;
import org.gbif.registry.ws.resources.NodeResource;
import org.gbif.registry.ws.resources.OccurrenceDownloadResource;
import org.gbif.registry.ws.resources.OrganizationResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.gbif.registry.guice.RegistryTestModules.webservice;
import static org.gbif.registry.guice.RegistryTestModules.webserviceClient;

import static org.junit.Assert.assertEquals;

/**
 * Runs tests for the {@link OccurrenceDownloadService} implementations.
 * This is parameterized to run the same test routines for the following:
 * <ol>
 * <li>The persistence layer</li>
 * <li>The WS service layer</li>
 * <li>The WS service client layer</li>
 * </ol>
 */
@RunWith(Parameterized.class)
public class DatasetOccurrenceDownloadIT {

  // Flushes the database on each run
  @ClassRule
  public static final LiquibaseInitializer liquibaseRule = new LiquibaseInitializer(LiquibaseModules.database());

  @ClassRule
  public static final RegistryServer registryServer = RegistryServer.INSTANCE;

  // Tests user
  private static String TEST_USER = "admin";

  @Parameters
  public static Iterable<Object[]> data() {
    final Injector webservice = webservice();
    final Injector client = webserviceClient();
    return ImmutableList.<Object[]>of(new Object[] {webservice.getInstance(OccurrenceDownloadResource.class),
      webservice.getInstance(DatasetResource.class), webservice.getInstance(OrganizationResource.class),
      webservice.getInstance(NodeResource.class), webservice.getInstance(InstallationResource.class),
      null, webservice.getInstance(DatasetOccurrenceDownloadUsageResource.class)},
      new Object[] {client.getInstance(OccurrenceDownloadService.class), client.getInstance(DatasetService.class),
        client.getInstance(OrganizationService.class), client.getInstance(NodeService.class),
        client.getInstance(InstallationService.class), client.getInstance(SimplePrincipalProvider.class),
        client.getInstance(DatasetOccurrenceDownloadUsageService.class)});
  }

  @Rule
  public final DatabaseInitializer databaseRule = new DatabaseInitializer(LiquibaseModules.database());

  private final OccurrenceDownloadService occurrenceDownloadService;

  private final DatasetOccurrenceDownloadUsageService datasetOccurrenceDownloadUsageService;

  private final SimplePrincipalProvider simplePrincipalProvider;

  // The following services are required to create dataset instances
  private final DatasetService datasetService;
  private final OrganizationService organizationService;
  private final NodeService nodeService;
  private final InstallationService installationService;

  @Before
  public void setup() {
    // reset SimplePrincipleProvider, configured for web service client tests only
    if (simplePrincipalProvider != null) {
      simplePrincipalProvider.setPrincipal(TEST_USER);
    }
  }

  public DatasetOccurrenceDownloadIT(
    OccurrenceDownloadService occurrenceDownloadService,
    DatasetService datasetService,
    OrganizationService organizationService, NodeService nodeService,
    InstallationService installationService,
    SimplePrincipalProvider simplePrincipalProvider,
    DatasetOccurrenceDownloadUsageService datasetOccurrenceDownloadUsageService) {
    this.occurrenceDownloadService = occurrenceDownloadService;
    this.datasetService = datasetService;
    this.organizationService = organizationService;
    this.nodeService = nodeService;
    this.installationService = installationService;
    this.simplePrincipalProvider = simplePrincipalProvider;
    this.datasetOccurrenceDownloadUsageService = datasetOccurrenceDownloadUsageService;
  }

  /**
   * Creates a test dataset. The dataset is persisted in the data base.
   * The installation and organization related to the dataset are created too.
   */
  private Dataset createTestDataset() {
    // endorsing node for the organization
    UUID nodeKey = nodeService.create(Nodes.newInstance());

    // publishing organization (required field)
    Organization o = Organizations.newInstance(nodeKey);
    UUID organizationKey = organizationService.create(o);

    Installation i = Installations.newInstance(organizationKey);
    UUID installationKey = installationService.create(i);
    Dataset dataset = Datasets.newInstance(organizationKey, installationKey);
    dataset.setKey(datasetService.create(dataset));
    return dataset;
  }

  /**
   * Tests the process of persist a {@link DatasetOccurrenceDownload} and list the downloads by dataset key.
   */
  @Test
  public void testAddAndGetOccurrenceDatasetOne() {
    Download occurrenceDownload = OccurrenceDownloadIT.getTestInstancePredicateDownload();
    final Dataset testDataset = createTestDataset();
    
    occurrenceDownloadService.create(occurrenceDownload);
    Map<UUID,Long> datasetCitation = new HashMap<>();
    datasetCitation.put(testDataset.getKey(), 1000L);
    occurrenceDownloadService.createUsages(occurrenceDownload.getKey(), datasetCitation);
    
    assertEquals("List operation should return 1 record", 1,
      datasetOccurrenceDownloadUsageService.listByDataset(testDataset.getKey(), new PagingRequest(0, 3))
        .getResults().size());
    Download occDownload2 = occurrenceDownloadService.get(occurrenceDownload.getKey());
    assertEquals(1,occDownload2.getNumberDatasets());
  }
  
  /**
   * Tests the process of persist a list of {@link DatasetOccurrenceDownload} and list the downloads by dataset key.
   */
  @Test
  public void testAddAndGetOccurrenceDatasetMany() {
    Download occurrenceDownload = OccurrenceDownloadIT.getTestInstancePredicateDownload();
    final Dataset testDataset1 = createTestDataset();
    final Dataset testDataset2 = createTestDataset();
    final Dataset testDataset3 = createTestDataset();
    
    occurrenceDownloadService.create(occurrenceDownload);
    
    Map<UUID,Long> datasetCitation = new HashMap<>();
    datasetCitation.put(testDataset1.getKey(), 1000L);
    datasetCitation.put(testDataset2.getKey(), 10000L);
    datasetCitation.put(testDataset3.getKey(), 100000L);
    occurrenceDownloadService.createUsages(occurrenceDownload.getKey(), datasetCitation);
    
    assertEquals("List operation should return 1 record", 1,
      datasetOccurrenceDownloadUsageService.listByDataset(testDataset1.getKey(), new PagingRequest(0, 3))
        .getResults().size());
    assertEquals("List operation should return 1 record", 1,
        datasetOccurrenceDownloadUsageService.listByDataset(testDataset2.getKey(), new PagingRequest(0, 3))
          .getResults().size());
    assertEquals("List operation should return 1 record", 1,
        datasetOccurrenceDownloadUsageService.listByDataset(testDataset3.getKey(), new PagingRequest(0, 3))
          .getResults().size());
    Download occDownload2 = occurrenceDownloadService.get(occurrenceDownload.getKey());
    assertEquals(3,occDownload2.getNumberDatasets());
  }

}
