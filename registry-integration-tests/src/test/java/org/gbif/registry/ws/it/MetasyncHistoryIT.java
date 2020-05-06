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
package org.gbif.registry.ws.it;

import org.gbif.api.model.common.paging.Pageable;
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
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.client.InstallationClient;
import org.gbif.registry.ws.it.fixtures.TestConstants;
import org.gbif.registry.ws.it.fixtures.UserTestFixture;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs tests for the {@link MetasyncHistoryService} implementations. This is parameterized to run
 * the same test routines for the following:
 *
 * <ol>
 *   <li>The WS service layer
 *   <li>The WS service client layer
 * </ol>
 */
public class MetasyncHistoryIT extends BaseItTest {

  private final MetasyncHistoryService metasyncHistoryResource;
  private final MetasyncHistoryService metasyncHistoryClient;

  private final OrganizationService organizationService;
  private final NodeService nodeService;
  private final InstallationService installationService;

  private final TestDataFactory testDataFactory;
  private final UserTestFixture userTestFixture;

  @Autowired
  public MetasyncHistoryIT(
      MetasyncHistoryService metasyncHistoryResource,
      OrganizationService organizationService,
      NodeService nodeService,
      InstallationService installationService,
      SimplePrincipalProvider simplePrincipalProvider,
      TestDataFactory testDataFactory,
      EsManageServer esServer,
      @LocalServerPort int localServerPort,
      KeyStore keyStore,
      UserTestFixture userTestFixture) {
    super(simplePrincipalProvider, esServer);
    this.metasyncHistoryResource = metasyncHistoryResource;
    this.organizationService = organizationService;
    this.nodeService = nodeService;
    this.installationService = installationService;
    this.testDataFactory = testDataFactory;
    this.metasyncHistoryClient =
        prepareClient(
            TestConstants.TEST_ADMIN, localServerPort, keyStore, InstallationClient.class);
    this.userTestFixture = userTestFixture;
  }

  @BeforeEach
  public void beforeEach() {
    userTestFixture.prepareAdminUser();
  }

  /** Tests the operations create and list of {@link MetasyncHistoryService}. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testCreateAndList(ServiceType serviceType) {
    MetasyncHistoryService service =
        getService(serviceType, metasyncHistoryResource, metasyncHistoryClient);
    MetasyncHistory metasyncHistory = getTestInstance();
    service.createMetasync(metasyncHistory);
    PagingResponse<MetasyncHistory> response = service.listMetasync(new PagingRequest());
    assertTrue(
        response.getResults().size() > 0, "The list operation should return at least 1 record");
  }

  /** Tests the {@link MetasyncHistoryService#listMetasync(UUID, Pageable)} operation. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testListAndListByInstallation(ServiceType serviceType) {
    MetasyncHistoryService service =
        getService(serviceType, metasyncHistoryResource, metasyncHistoryClient);
    MetasyncHistory metasyncHistory = getTestInstance();
    service.createMetasync(metasyncHistory);
    PagingResponse<MetasyncHistory> response =
        service.listMetasync(metasyncHistory.getInstallationKey(), new PagingRequest());
    assertTrue(
        response.getResults().size() > 0, "The list operation should return at least 1 record");
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
