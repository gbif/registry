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
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.client.InstallationClient;
import org.gbif.ws.client.ClientFactory;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is parameterized to run the same test routines for the following:
 *
 * <ol>
 *   <li>The persistence layer
 *   <li>The WS service layer
 *   <li>The WS service client layer
 * </ol>
 */
public class InstallationIT extends NetworkEntityIT<Installation> {

  private final OrganizationService organizationService;
  private final NodeService nodeService;
  private final TestDataFactory testDataFactory;

  @Autowired
  public InstallationIT(
      InstallationService service,
      OrganizationService organizationService,
      NodeService nodeService,
      @Nullable SimplePrincipalProvider principalProvider,
      TestDataFactory testDataFactory,
      EsManageServer esServer,
      @LocalServerPort int localServerPort,
      KeyStore keyStore) {
    super(
        service,
        new ClientFactory(
                "gbif.app.it",
                "http://localhost:" + localServerPort,
                "gbif.app.it",
                keyStore.getPrivateKey("gbif.app.it"))
            .newInstance(InstallationClient.class),
        principalProvider,
        testDataFactory,
        esServer);
    this.organizationService = organizationService;
    this.nodeService = nodeService;
    this.testDataFactory = testDataFactory;
  }

  @Override
  protected Installation newEntity() {
    UUID nodeKey = nodeService.create(testDataFactory.newNode());
    Organization o = testDataFactory.newOrganization(nodeKey);
    UUID key = organizationService.create(o);
    Organization organization = organizationService.get(key);
    return testDataFactory.newInstallation(organization.getKey());
  }

  /** Tests that we can successfully disable and undisable an installation. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void disableInstallation(ServiceType serviceType) {
    Installation e = newEntity();
    InstallationService service = (InstallationService) getService(serviceType);
    UUID key = service.create(e);
    e = service.get(key);
    assertFalse(e.isDisabled(), "Should not be disabled to start");

    e.setDisabled(true);
    service.update(e);
    e = service.get(e.getKey());
    assertTrue(e.isDisabled(), "We just disabled it");

    e.setDisabled(false);
    service.update(e);
    e = service.get(e.getKey());
    assertFalse(e.isDisabled(), "We just un-disabled it");
  }

  // TODO: 04/05/2020 client test
  // Easier to test this here than other places due to our utility factory
  @Test
  public void testHostedByInstallationList() {
    Installation installation = create(newEntity(), 1);
    Organization organization = organizationService.get(installation.getOrganizationKey());
    Node node = nodeService.get(organization.getEndorsingNodeKey());

    PagingResponse<Installation> resp =
        nodeService.installations(node.getKey(), new PagingRequest());
    assertEquals(Long.valueOf(1), resp.getCount(), "Paging counts are not being set");

    resp = organizationService.installations(organization.getKey(), new PagingRequest());
    assertEquals(Long.valueOf(1), resp.getCount(), "Paging counts are not being set");
  }

  @Override
  protected Installation duplicateForCreateAsEditorTest(Installation entity) throws Exception {
    Installation duplicate = (Installation) BeanUtils.cloneBean(entity);
    duplicate.setOrganizationKey(entity.getOrganizationKey());
    return duplicate;
  }

  @Override
  protected UUID keyForCreateAsEditorTest(Installation entity) {
    return organizationService.get(entity.getOrganizationKey()).getEndorsingNodeKey();
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testSuggest(ServiceType serviceType) {
    InstallationService service = (InstallationService) getService(serviceType);
    Installation installation1 = newEntity();
    installation1.setTitle("The installation");
    service.create(installation1);

    Installation installation2 = newEntity();
    installation2.setTitle("The Great installation");
    service.create(installation2);

    assertEquals(1, service.suggest("Great").size(), "Should find only The Great installation");
    assertEquals(2, service.suggest("the").size(), "Should find both installations");
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testListByType(ServiceType serviceType) {
    InstallationService service = (InstallationService) getService(serviceType);
    Installation installation1 = newEntity();
    installation1.setTitle("The installation");
    installation1.setType(InstallationType.HTTP_INSTALLATION);
    service.create(installation1);

    Installation installation2 = newEntity();
    installation2.setTitle("The Great installation");
    installation2.setType(InstallationType.EARTHCAPE_INSTALLATION);
    service.create(installation2);

    assertEquals(
        1,
        service
            .listByType(InstallationType.EARTHCAPE_INSTALLATION, new PagingRequest())
            .getResults()
            .size(),
        "Should find only The Great installation");
  }
}
