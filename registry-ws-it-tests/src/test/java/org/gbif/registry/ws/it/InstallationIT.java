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
import org.gbif.registry.test.TestDataFactory;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.beanutils.BeanUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

/**
 * This is parameterized to run the same test routines for the following:
 *
 * <ol>
 *   <li>The persistence layer
 *   <li>The WS service layer
 *   <li>The WS service client layer
 * </ol>
 */
@RunWith(Parameterized.class)
public class InstallationIT extends NetworkEntityTest<Installation> {

  private final OrganizationService organizationService;
  private final NodeService nodeService;
  private final TestDataFactory testDataFactory;

  @Autowired
  public InstallationIT(
      InstallationService service,
      OrganizationService organizationService,
      NodeService nodeService,
      @Nullable SimplePrincipalProvider pp,
      TestDataFactory testDataFactory) {
    super(service, pp, testDataFactory);
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
    Installation i = testDataFactory.newInstallation(organization.getKey());
    return i;
  }

  /** Tests that we can successfully disable and undisable an installation. */
  @Test
  public void disableInstallation() {
    Installation e = newEntity();
    UUID key = getService().create(e);
    e = getService().get(key);
    assertEquals("Should not be disabled to start", false, e.isDisabled());
    e.setDisabled(true);
    getService().update(e);
    e = getService().get(e.getKey());
    assertEquals("We just disabled it", true, e.isDisabled());
    e.setDisabled(false);
    getService().update(e);
    e = getService().get(e.getKey());
    assertEquals("We just un-disabled it", false, e.isDisabled());
  }

  // Easier to test this here than other places due to our utility factory
  @Test
  public void testHostedByInstallationList() {
    Installation installation = create(newEntity(), 1);
    Organization organization = organizationService.get(installation.getOrganizationKey());
    Node node = nodeService.get(organization.getEndorsingNodeKey());

    PagingResponse<Installation> resp =
        nodeService.installations(node.getKey(), new PagingRequest());
    assertEquals("Paging counts are not being set", Long.valueOf(1), resp.getCount());

    resp = organizationService.installations(organization.getKey(), new PagingRequest());
    assertEquals("Paging counts are not being set", Long.valueOf(1), resp.getCount());
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

  @Test
  public void testSuggest() {
    Installation installation1 = newEntity();
    installation1.setTitle("The installation");
    UUID key1 = getService().create(installation1);

    Installation installation2 = newEntity();
    installation2.setTitle("The Great installation");
    UUID key2 = getService().create(installation2);

    InstallationService service = (InstallationService) this.getService();
    assertEquals("Should find only The Great installation", 1, service.suggest("Great").size());
    assertEquals("Should find both installations", 2, service.suggest("the").size());
  }

  @Test
  public void testListByType() {
    Installation installation1 = newEntity();
    installation1.setTitle("The installation");
    installation1.setType(InstallationType.HTTP_INSTALLATION);
    UUID key1 = getService().create(installation1);

    Installation installation2 = newEntity();
    installation2.setTitle("The Great installation");
    installation2.setType(InstallationType.EARTHCAPE_INSTALLATION);
    UUID key2 = getService().create(installation2);

    InstallationService service = (InstallationService) this.getService();
    assertEquals(
        "Should find only The Great installation",
        1,
        service.listByType(InstallationType.EARTHCAPE_INSTALLATION, null).getResults().size());
  }
}
