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
import org.gbif.registry.test.TestDataFactory;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.beanutils.BeanUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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

  private final OrganizationService service;
  private final NodeService nodeService;

  private final TestDataFactory testDataFactory;

  @Autowired
  public OrganizationIT(
      OrganizationService service,
      NodeService nodeService,
      @Nullable SimplePrincipalProvider pp,
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
    assertEquals(1, service.suggest("The").size(), "Should find only The Tim");
    assertEquals(2, service.suggest("Tim").size(), "Should find both organizations");
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
        Long.valueOf(1),
        nodeService.pendingEndorsements(new PagingRequest()).getCount(),
        "Paging is not returning the correct count");

    o.setEndorsementApproved(true);
    this.getService().update(o);
    assertResultsOfSize(nodeService.pendingEndorsements(new PagingRequest()), 0);
    assertEquals(
        Long.valueOf(0),
        nodeService.pendingEndorsements(new PagingRequest()).getCount(),
        "Paging is not returning the correct count");
    assertResultsOfSize(nodeService.endorsedOrganizations(node.getKey(), new PagingRequest()), 1);
    assertEquals(
        Long.valueOf(1),
        nodeService.endorsedOrganizations(node.getKey(), new PagingRequest()).getCount(),
        "Paging is not returning the correct count");
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
        Long.valueOf(2),
        service.listByCountry(Country.ANGOLA, new PagingRequest()).getCount(),
        "Paging is not returning the correct count");
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
