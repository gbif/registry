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
package org.gbif.registry.ws;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.metasync.MetasyncHistory;
import org.gbif.api.model.registry.metasync.MetasyncResult;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.MetasyncHistoryService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Date;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;

/**
 * Runs tests for the {@link MetasyncHistoryService} implementations. This is parameterized to run
 * the same test routines for the following:
 *
 * <ol>
 *   <li>The WS service layer
 *   <li>The WS service client layer
 * </ol>
 */
@RunWith(Parameterized.class)
public class MetasyncHistoryIT {

  @RegisterExtension
  static PreparedDbExtension database =
      EmbeddedPostgresExtension.preparedDatabase(
          LiquibasePreparer.forClasspathLocation("liquibase/master.xml"));

  @RegisterExtension
  public final DatabaseInitializer databaseRule =
      new DatabaseInitializer(database.getTestDatabase());

  // Tests user
  private static String TEST_USER = "admin";

  private final MetasyncHistoryService metasyncHistoryService;

  private final SimplePrincipalProvider simplePrincipalProvider;

  private final OrganizationService organizationService;
  private final NodeService nodeService;
  private final InstallationService installationService;

  private final TestDataFactory testDataFactory;

  @Autowired
  public MetasyncHistoryIT(
      MetasyncHistoryService metasyncHistoryService,
      OrganizationService organizationService,
      NodeService nodeService,
      InstallationService installationService,
      SimplePrincipalProvider simplePrincipalProvider,
      TestDataFactory testDataFactory) {
    this.metasyncHistoryService = metasyncHistoryService;
    this.organizationService = organizationService;
    this.nodeService = nodeService;
    this.installationService = installationService;
    this.simplePrincipalProvider = simplePrincipalProvider;
    this.testDataFactory = testDataFactory;
  }

  @Before
  public void setup() {
    // reset SimplePrincipleProvider, configured for web service client tests only
    if (simplePrincipalProvider != null) {
      simplePrincipalProvider.setPrincipal(TEST_USER);
    }
  }

  /** Tests the operations create and list of {@link MetasyncHistoryService}. */
  @Test
  public void testCreateAndList() {
    MetasyncHistory metasyncHistory = getTestInstance();
    metasyncHistoryService.createMetasync(metasyncHistory);
    PagingResponse<MetasyncHistory> response =
        metasyncHistoryService.listMetasync(new PagingRequest());
    Assert.assertTrue(
        "The list operation should return at least 1 record", response.getResults().size() > 0);
  }

  /**
   * Tests the {@link MetasyncHistoryService#listByInstallation(UUID,
   * org.gbif.api.model.common.paging.Pageable)} operation.
   */
  @Test
  public void testListAndListByInstallation() {
    MetasyncHistory metasyncHistory = getTestInstance();
    metasyncHistoryService.createMetasync(metasyncHistory);
    PagingResponse<MetasyncHistory> response =
        metasyncHistoryService.listMetasync(
            metasyncHistory.getInstallationKey(), new PagingRequest());
    Assert.assertTrue(
        "The list operation should return at least 1 record", response.getResults().size() > 0);
  }

  /**
   * Creates a test installation. The installation is persisted in the data base. The organization
   * related to the installation are created too.
   */
  private Installation createTestInstallation() {
    // endorsing node for the organization
    UUID nodeKey = nodeService.create(testDataFactory.newNode());

    // publishing organization (required field)
    Organization org = testDataFactory.newOrganization(nodeKey);
    UUID organizationKey = organizationService.create(org);

    Installation inst = testDataFactory.newInstallation(organizationKey);
    UUID installationKey = installationService.create(inst);
    inst.setKey(installationKey);
    return inst;
  }

  /** Creates {@link MetasyncHistory} object to be used in test cases. */
  private MetasyncHistory getTestInstance() {
    MetasyncHistory metasyncHistory = new MetasyncHistory();
    metasyncHistory.setDetails("testDetails");
    metasyncHistory.setResult(MetasyncResult.OK);
    metasyncHistory.setSyncDate(new Date());
    Installation installation = createTestInstallation();
    metasyncHistory.setInstallationKey(installation.getKey());
    return metasyncHistory;
  }
}
