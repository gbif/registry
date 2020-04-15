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
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.test.Nodes;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.beanutils.BeanUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
public class OrganizationIT extends NetworkEntityTest<Organization> {

  private final OrganizationService service;
  private final NodeService nodeService;

  private final TestDataFactory testDataFactory;

  @Inject
  public OrganizationIT(
      OrganizationService service,
      NodeService nodeService,
      @Nullable SimplePrincipalProvider pp,
      Nodes nodes,
      TestDataFactory testDataFactory) {
    super(service, pp, testDataFactory);
    this.service = service;
    this.nodeService = nodeService;
    this.testDataFactory = testDataFactory;
  }

  @Test
  public void testSuggest() {
    Node node = testDataFactory.newNode();
    UUID nodeKey = nodeService.create(node);

    Organization o1 = testDataFactory.newOrganization(nodeKey);
    o1.setTitle("Tim");
    UUID key1 = this.getService().create(o1);

    Organization o2 = testDataFactory.newOrganization(nodeKey);
    o2.setTitle("The Tim");
    UUID key2 = this.getService().create(o2);

    OrganizationService service = (OrganizationService) this.getService();
    assertTrue("Should find only The Tim", service.suggest("The").size() == 1);
    assertTrue("Should find both organizations", service.suggest("Tim").size() == 2);
  }

  @Test
  public void testEndorsements() {
    Node node = testDataFactory.newNode();
    nodeService.create(node);
    node = nodeService.list(new PagingRequest()).getResults().get(0);

    assertResultsOfSize(nodeService.endorsedOrganizations(node.getKey(), new PagingRequest()), 0);
    assertResultsOfSize(nodeService.pendingEndorsements(new PagingRequest()), 0);

    Organization o = testDataFactory.newOrganization(node.getKey());
    UUID key = this.getService().create(o);
    o = getService().get(key);
    assertResultsOfSize(nodeService.endorsedOrganizations(node.getKey(), new PagingRequest()), 0);
    assertResultsOfSize(nodeService.pendingEndorsements(new PagingRequest()), 1);
    assertResultsOfSize(nodeService.pendingEndorsements(node.getKey(), new PagingRequest()), 1);
    assertEquals(
        "Paging is not returning the correct count",
        Long.valueOf(1),
        nodeService.pendingEndorsements(new PagingRequest()).getCount());

    o.setEndorsementApproved(true);
    this.getService().update(o);
    assertResultsOfSize(nodeService.pendingEndorsements(new PagingRequest()), 0);
    assertEquals(
        "Paging is not returning the correct count",
        Long.valueOf(0),
        nodeService.pendingEndorsements(new PagingRequest()).getCount());
    assertResultsOfSize(nodeService.endorsedOrganizations(node.getKey(), new PagingRequest()), 1);
    assertEquals(
        "Paging is not returning the correct count",
        Long.valueOf(1),
        nodeService.endorsedOrganizations(node.getKey(), new PagingRequest()).getCount());
  }

  @Test
  public void testByCountry() {
    Node node = testDataFactory.newNode();
    nodeService.create(node);
    node = nodeService.list(new PagingRequest()).getResults().get(0);

    createOrgs(
        node.getKey(),
        Country.ANGOLA,
        Country.ANGOLA,
        Country.DENMARK,
        Country.FRANCE,
        Country.FRANCE,
        Country.UNKNOWN);

    assertResultsOfSize(service.listByCountry(Country.ANGOLA, new PagingRequest()), 2);
    assertEquals(
        "Paging is not returning the correct count",
        Long.valueOf(2),
        service.listByCountry(Country.ANGOLA, new PagingRequest()).getCount());
    assertResultsOfSize(service.listByCountry(Country.FRANCE, new PagingRequest()), 2);
    assertResultsOfSize(service.listByCountry(Country.GERMANY, new PagingRequest()), 0);
  }

  private void createOrgs(UUID nodeKey, Country... countries) {
    for (Country c : countries) {
      Organization o = testDataFactory.newOrganization(nodeKey);
      o.setCountry(c);
      o.setKey(service.create(o));
    }
  }

  @Override
  protected Organization newEntity() {
    UUID key = nodeService.create(testDataFactory.newNode());
    Node node = nodeService.get(key);
    Organization o = testDataFactory.newOrganization(node.getKey());
    return o;
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
