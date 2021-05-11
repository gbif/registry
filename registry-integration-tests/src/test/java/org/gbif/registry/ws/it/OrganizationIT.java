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

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.domain.ws.OrganizationRequestSearchParams;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.client.NodeClient;
import org.gbif.registry.ws.client.OrganizationClient;
import org.gbif.registry.ws.resources.OrganizationResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.beanutils.BeanUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This is parameterized to run the same test routines for the following:
 *
 * <ol>
 *   <li>The persistence layer
 *   <li>The WS service layer
 *   <li>The WS service client layer
 * </ol>
 */
public class OrganizationIT extends NetworkEntityIT<Organization> {

  private final NodeService nodeResource;
  private final NodeService nodeClient;
  private final NetworkService networkService;
  private final OrganizationResource organizationResource;

  private final TestDataFactory testDataFactory;

  @Autowired
  public OrganizationIT(
      OrganizationService service,
      NodeService nodeResource,
      NetworkService networkService,
      OrganizationResource organizationResource,
      @Nullable SimplePrincipalProvider principalProvider,
      TestDataFactory testDataFactory,
      EsManageServer esServer,
      @LocalServerPort int localServerPort,
      KeyStore keyStore) {
    super(
        service,
        localServerPort,
        keyStore,
        OrganizationClient.class,
        principalProvider,
        testDataFactory,
        esServer);
    this.nodeResource = nodeResource;
    this.nodeClient = prepareClient(localServerPort, keyStore, NodeClient.class);
    this.testDataFactory = testDataFactory;
    this.networkService = networkService;
    this.organizationResource = organizationResource;
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testSuggest(ServiceType serviceType) {
    OrganizationService service = (OrganizationService) getService(serviceType);
    Node node = testDataFactory.newNode();
    UUID nodeKey = nodeResource.create(node);

    Organization o1 = testDataFactory.newOrganization(nodeKey);
    o1.setTitle("Tim");
    service.create(o1);

    Organization o2 = testDataFactory.newOrganization(nodeKey);
    o2.setTitle("The Tim");
    service.create(o2);

    assertEquals(1, service.suggest("The").size(), "Should find only The Tim");
    assertEquals(2, service.suggest("Tim").size(), "Should find both organizations");
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testByCountry(ServiceType serviceType) {
    OrganizationService service = (OrganizationService) getService(serviceType);
    NodeService nodeService = getService(serviceType, nodeResource, nodeClient);

    Node node = testDataFactory.newNode();
    nodeService.create(node);
    node = nodeService.list(new PagingRequest()).getResults().get(0);

    createOrgs(
        node.getKey(),
        serviceType,
        Country.ANGOLA,
        Country.ANGOLA,
        Country.DENMARK,
        Country.FRANCE,
        Country.FRANCE,
        Country.UNKNOWN);

    assertResultsOfSize(service.listByCountry(Country.ANGOLA, new PagingRequest()), 2);
    assertEquals(
        Long.valueOf(2),
        service.listByCountry(Country.ANGOLA, new PagingRequest()).getCount(),
        "Paging is not returning the correct count");
    assertResultsOfSize(service.listByCountry(Country.FRANCE, new PagingRequest()), 2);
    assertResultsOfSize(service.listByCountry(Country.GERMANY, new PagingRequest()), 0);
  }

  @Test
  public void searchByNetworkTest() {
    Installation installation = testDataFactory.newPersistedInstallation();
    Dataset dataset =
        testDataFactory.newPersistedDataset(
            installation.getOrganizationKey(), installation.getKey());
    Network network = testDataFactory.newPersistedNetwork();

    OrganizationRequestSearchParams searchParams = new OrganizationRequestSearchParams();
    searchParams.setNetworkKey(network.getKey());
    PagingResponse<Organization> response = organizationResource.list(null, searchParams, null);

    assertResultsOfSize(response, 0);

    networkService.addConstituent(network.getKey(), dataset.getKey());
    response = organizationResource.list(null, searchParams, null);

    assertResultsOfSize(response, 1);
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testHostedByInstallationList(ServiceType serviceType) {
    OrganizationService service = (OrganizationService) getService(serviceType);
    NodeService nodeService = getService(serviceType, nodeResource, nodeClient);

    Installation installation = testDataFactory.newPersistedInstallation();
    Organization organization = service.get(installation.getOrganizationKey());
    Node node = nodeService.get(organization.getEndorsingNodeKey());

    PagingResponse<Installation> resp =
        nodeService.installations(node.getKey(), new PagingRequest());
    assertResultsOfSize(resp, 1);

    resp = service.installations(organization.getKey(), new PagingRequest());
    assertResultsOfSize(resp, 1);
  }

  private void createOrgs(UUID nodeKey, ServiceType serviceType, Country... countries) {
    OrganizationService service = (OrganizationService) getService(serviceType);
    for (Country c : countries) {
      Organization o = testDataFactory.newOrganization(nodeKey);
      o.setCountry(c);
      o.setKey(service.create(o));
    }
  }

  @Override
  protected Organization newEntity(ServiceType serviceType) {
    NodeService nodeService = getService(serviceType, nodeResource, nodeClient);

    UUID key = nodeService.create(testDataFactory.newNode());
    Node node = nodeService.get(key);
    return testDataFactory.newOrganization(node.getKey());
  }

  @Override
  protected Organization duplicateForCreateAsEditorTest(Organization entity) throws Exception {
    Organization duplicate = (Organization) BeanUtils.cloneBean(entity);
    duplicate.setEndorsingNodeKey(entity.getEndorsingNodeKey());
    return duplicate;
  }

  @Override
  protected UUID keyForCreateAsEditorTest(Organization entity) {
    return entity.getEndorsingNodeKey();
  }
}
