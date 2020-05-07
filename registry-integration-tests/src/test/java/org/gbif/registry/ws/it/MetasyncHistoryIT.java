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
import org.gbif.registry.ws.client.NodeClient;
import org.gbif.registry.ws.client.OrganizationClient;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.util.Date;
import java.util.UUID;

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

  private final OrganizationService organizationResource;
  private final OrganizationService organizationClient;
  private final NodeService nodeResource;
  private final NodeService nodeClient;
  private final InstallationService installationResource;
  private final InstallationService installationClient;

  private final TestDataFactory testDataFactory;

  @Autowired
  public MetasyncHistoryIT(
      MetasyncHistoryService metasyncHistoryResource,
      OrganizationService organizationResource,
      NodeService nodeResource,
      InstallationService installationResource,
      SimplePrincipalProvider simplePrincipalProvider,
      TestDataFactory testDataFactory,
      EsManageServer esServer,
      @LocalServerPort int localServerPort,
      KeyStore keyStore) {
    super(simplePrincipalProvider, esServer);
    this.metasyncHistoryResource = metasyncHistoryResource;
    this.organizationResource = organizationResource;
    this.organizationClient = prepareClient(localServerPort, keyStore, OrganizationClient.class);
    this.nodeResource = nodeResource;
    this.nodeClient = prepareClient(localServerPort, keyStore, NodeClient.class);
    this.installationResource = installationResource;
    this.installationClient = prepareClient(localServerPort, keyStore, InstallationClient.class);
    this.testDataFactory = testDataFactory;
    this.metasyncHistoryClient = prepareClient(localServerPort, keyStore, InstallationClient.class);
  }

  /** Tests the operations create and list of {@link MetasyncHistoryService}. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testCreateAndList(ServiceType serviceType) {
    MetasyncHistoryService service =
        getService(serviceType, metasyncHistoryResource, metasyncHistoryClient);
    MetasyncHistory metasyncHistory = getTestInstance(serviceType);
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
    MetasyncHistory metasyncHistory = getTestInstance(serviceType);
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
  private Installation createTestInstallation(ServiceType serviceType) {
    NodeService nodeService = getService(serviceType, nodeResource, nodeClient);
    OrganizationService organizationService =
        getService(serviceType, organizationResource, organizationClient);
    InstallationService installationService =
        getService(serviceType, installationResource, installationClient);

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
  private MetasyncHistory getTestInstance(ServiceType serviceType) {
    MetasyncHistory metasyncHistory = new MetasyncHistory();
    metasyncHistory.setDetails("testDetails");
    metasyncHistory.setResult(MetasyncResult.OK);
    metasyncHistory.setSyncDate(new Date());
    Installation installation = createTestInstallation(serviceType);
    metasyncHistory.setInstallationKey(installation.getKey());
    return metasyncHistory;
  }
}
