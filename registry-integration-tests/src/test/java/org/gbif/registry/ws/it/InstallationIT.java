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
package org.gbif.registry.ws.it;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.client.InstallationClient;
import org.gbif.registry.ws.client.NodeClient;
import org.gbif.registry.ws.client.OrganizationClient;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.beanutils.BeanUtils;
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

  private final OrganizationService organizationResource;
  private final OrganizationService organizationClient;
  private final NodeService nodeResource;
  private final NodeService nodeClient;

  private final TestDataFactory testDataFactory;

  @Autowired
  public InstallationIT(
      InstallationService service,
      OrganizationService organizationResource,
      NodeService nodeResource,
      @Nullable SimplePrincipalProvider principalProvider,
      TestDataFactory testDataFactory,
      EsManageServer esServer,
      @LocalServerPort int localServerPort,
      KeyStore keyStore) {
    super(
        service,
        localServerPort,
        keyStore,
        InstallationClient.class,
        principalProvider,
        testDataFactory,
        esServer);
    this.organizationResource = organizationResource;
    this.organizationClient = prepareClient(localServerPort, keyStore, OrganizationClient.class);
    this.nodeResource = nodeResource;
    this.nodeClient = prepareClient(localServerPort, keyStore, NodeClient.class);
    this.testDataFactory = testDataFactory;
  }

  /** Tests that we can successfully disable and undisable an installation. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void disableInstallation(ServiceType serviceType) {
    Installation e = newEntity(serviceType);
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

  @Override
  protected Installation duplicateForCreateAsEditorTest(Installation entity) throws Exception {
    Installation duplicate = (Installation) BeanUtils.cloneBean(entity);
    duplicate.setOrganizationKey(entity.getOrganizationKey());
    return duplicate;
  }

  @Override
  protected UUID keyForCreateAsEditorTest(Installation entity) {
    return organizationResource.get(entity.getOrganizationKey()).getEndorsingNodeKey();
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testSuggest(ServiceType serviceType) {
    InstallationService service = (InstallationService) getService(serviceType);
    Installation installation1 = newEntity(serviceType);
    installation1.setTitle("The installation");
    service.create(installation1);

    Installation installation2 = newEntity(serviceType);
    installation2.setTitle("The Great installation");
    service.create(installation2);

    assertEquals(1, service.suggest("Great").size(), "Should find only The Great installation");
    assertEquals(2, service.suggest("the").size(), "Should find both installations");
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testListByType(ServiceType serviceType) {
    InstallationService service = (InstallationService) getService(serviceType);
    Installation installation1 = newEntity(serviceType);
    installation1.setTitle("The installation");
    installation1.setType(InstallationType.HTTP_INSTALLATION);
    service.create(installation1);

    Installation installation2 = newEntity(serviceType);
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

  @Override
  protected Installation newEntity(ServiceType serviceType) {
    NodeService nodeService = getService(serviceType, nodeResource, nodeClient);
    OrganizationService organizationService =
        getService(serviceType, organizationResource, organizationClient);

    UUID nodeKey = nodeService.create(testDataFactory.newNode());
    Organization o = testDataFactory.newOrganization(nodeKey);
    UUID key = organizationService.create(o);
    Organization organization = organizationService.get(key);
    return testDataFactory.newInstallation(organization.getKey());
  }
}
