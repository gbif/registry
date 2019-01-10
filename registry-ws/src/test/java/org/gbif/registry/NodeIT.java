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

import org.apache.commons.beanutils.BeanUtils;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.GbifRegion;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.NodeType;
import org.gbif.api.vocabulary.ParticipationStatus;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.utils.Datasets;
import org.gbif.registry.utils.Installations;
import org.gbif.registry.utils.Nodes;
import org.gbif.registry.utils.Organizations;
import org.gbif.registry.ws.resources.DatasetResource;
import org.gbif.registry.ws.resources.InstallationResource;
import org.gbif.registry.ws.resources.NodeResource;
import org.gbif.registry.ws.resources.OrganizationResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * This is parameterized to run the same test routines for the following:
 * <ol>
 * <li>The persistence layer</li>
 * <li>The WS service layer</li>
 * <li>The WS service client layer</li>
 * </ol>
 */
@RunWith(Parameterized.class)
public class NodeIT extends NetworkEntityTest<Node> {

  private final NodeService nodeService;
  private final OrganizationService organizationService;
  private final InstallationService installationService;
  private final DatasetService datasetService;
  private int count;

  private static final Map<Country, Integer> TEST_COUNTRIES = ImmutableMap.<Country, Integer>builder()
    .put(Country.AFGHANISTAN, 6)
    .put(Country.ARGENTINA, 16)
    .put(Country.DENMARK, 2)
    .put(Country.SPAIN, 1)
    .build();

  @Parameters
  public static Iterable<Object[]> data() {
    return ImmutableList.<Object[]>of(
      new Object[] {
        RegistryTestModules.webservice().getInstance(NodeResource.class),
        RegistryTestModules.webservice().getInstance(OrganizationResource.class),
        RegistryTestModules.webservice().getInstance(InstallationResource.class),
        RegistryTestModules.webservice().getInstance(DatasetResource.class),
        null
      },
      new Object[] {
        RegistryTestModules.webserviceClient().getInstance(NodeService.class),
        RegistryTestModules.webserviceClient().getInstance(OrganizationService.class),
        RegistryTestModules.webserviceClient().getInstance(InstallationService.class),
        RegistryTestModules.webserviceClient().getInstance(DatasetService.class),
        RegistryTestModules.webserviceClient().getInstance(SimplePrincipalProvider.class)
      });
  }

  public NodeIT(NodeService nodeService, OrganizationService organizationService,
    InstallationService installationService, DatasetService datasetService, @Nullable SimplePrincipalProvider pp) {
    super(nodeService, pp);
    this.nodeService = nodeService;
    this.organizationService = organizationService;
    this.installationService = installationService;
    this.datasetService = datasetService;
  }

  //@Ignore("Problems with IMS connection. See issue: http://dev.gbif.org/issues/browse/REG-407")
  @Test
  public void testGetByCountry() {
    initVotingCountryNodes();
    insertTestNode(Country.TAIWAN, ParticipationStatus.ASSOCIATE, NodeType.OTHER);

    Node n = nodeService.getByCountry(Country.ANGOLA);
    assertNull(n);

    for (Country c : TEST_COUNTRIES.keySet()) {
      n = nodeService.getByCountry(c);
      assertEquals(c, n.getCountry());
    }

    // test taiwan hack
    n = nodeService.getByCountry(Country.TAIWAN);
    assertEquals(Country.TAIWAN, n.getCountry());
  }

  private void initVotingCountryNodes() {
    count = 0;
    for (Country c : TEST_COUNTRIES.keySet()) {
      insertTestNode(c, ParticipationStatus.VOTING, NodeType.COUNTRY);
    }
  }

  private void insertTestNode(Country c, ParticipationStatus status, NodeType nodeType) {
      Node n = newEntity();
      n.setCountry(c);
      n.setTitle("GBIF Node " + c.getTitle());
      n.setType(nodeType);
      n.setParticipationStatus(status);
      n.setGbifRegion(GbifRegion.AFRICA);
      n = create(n, count + 1);
      count++;

      if (TEST_COUNTRIES.containsKey(c)) {
        // create IMS identifiers
        Identifier id = new Identifier();
        id.setType(IdentifierType.GBIF_PARTICIPANT);
        id.setIdentifier(TEST_COUNTRIES.get(c).toString());
        nodeService.addIdentifier(n.getKey(), id);
      }
  }

  @Test
  public void testAffiliateNode(){
    Node n = newEntity();
    n.setTitle("GBIF Affiliate Node");
    n.setType(NodeType.OTHER);
    n.setParticipationStatus(ParticipationStatus.AFFILIATE);
    n.setGbifRegion(null);
    n.setCountry(null);
    create(n, 1);
  }

  //@Ignore("Problems with IMS connection. See issue: http://dev.gbif.org/issues/browse/REG-407")
  @Test
  public void testCountries() {
    initVotingCountryNodes();
    List<Country> countries = nodeService.listNodeCountries();
    assertEquals(TEST_COUNTRIES.size(), countries.size());
    for (Country c : countries) {
      assertTrue("Unexpected node country" + c, TEST_COUNTRIES.containsKey(c));
    }
  }

  @Test
  public void testActiveCountries() {
    initVotingCountryNodes();
    List<Country> countries = nodeService.listActiveCountries();
    assertEquals(TEST_COUNTRIES.size() + 1, countries.size());
    for (Country c : countries) {
      assertTrue("Unexpected node country" + c, Country.TAIWAN == c || TEST_COUNTRIES.containsKey(c));
    }
    assertTrue("Taiwan missing", countries.contains(Country.TAIWAN));

    // insert extra observer nodes and make sure we get the same list
    insertTestNode(Country.BOTSWANA, ParticipationStatus.OBSERVER, NodeType.COUNTRY);
    insertTestNode(Country.HONG_KONG, ParticipationStatus.FORMER, NodeType.COUNTRY);

    List<Country> countries2 = nodeService.listActiveCountries();
    assertEquals(countries, countries2);
  }

  @Test
  public void testDatasets() {
    // endorsing node for the organization
    Node node = create(newEntity(), 1);
    // publishing organization (required field)
    Organization o = Organizations.newInstance(node.getKey());
    o.setEndorsementApproved(true);
    o.setEndorsingNodeKey(node.getKey());
    UUID organizationKey = organizationService.create(o);
    // hosting technical installation (required field)
    Installation i = Installations.newInstance(organizationKey);
    UUID installationKey = installationService.create(i);
    // 2 datasets
    Dataset d1 = Datasets.newInstance(organizationKey, installationKey);
    datasetService.create(d1);
    Dataset d2 = Datasets.newInstance(organizationKey, installationKey);
    UUID d2Key = datasetService.create(d2);

    // test node service
    PagingResponse<Dataset> resp = nodeService.endorsedDatasets(node.getKey(), null);
    assertEquals(2, resp.getResults().size());
    assertEquals("Paging is not returning the correct count", Long.valueOf(2), resp.getCount());

    // the last created dataset should be the first in the list
    assertEquals(d2Key, resp.getResults().get(0).getKey());
  }

  //@Ignore("Problems with IMS connection. See issue: http://dev.gbif.org/issues/browse/REG-407")
  @Test
  /**
   * A test that requires a configured IMS with real spanish data.
   * Jenkins is configured for this, so we activate this test to make sure IMS connections are working!
   */
  public void testIms() throws Exception {
    initVotingCountryNodes();
    Node es = nodeService.getByCountry(Country.SPAIN);
    assertEquals((Integer) 2001, es.getParticipantSince());
    assertEquals(ParticipationStatus.VOTING, es.getParticipationStatus());
    // this is no real data, it comes from the test inserts above
    assertEquals(GbifRegion.AFRICA, es.getGbifRegion());
    assertEquals("GBIF.ES", es.getAbbreviation());
    assertEquals("Madrid", es.getCity());
    assertEquals("E-28014", es.getPostalCode());
    assertEquals("Real Jardín Botánico - CSIC", es.getOrganization());
    assertTrue(es.getContacts().size() > 5);

    Node notInIms = nodeService.getByCountry(Country.AFGHANISTAN);
    assertNotNull(notInIms);
  }

  /**
   * Node contacts are IMS managed and the service throws exceptions
   */
  @Override
  @Test(expected = UnsupportedOperationException.class)
  public void testContacts() {
    Node n = create(newEntity(), 1);
    nodeService.listContacts(n.getKey());
  }

  /**
   * Node contacts are IMS managed and the service throws exceptions
   */
  @Test(expected = UnsupportedOperationException.class)
  public void testAddContact() {
    Node n = create(newEntity(), 1);
    nodeService.addContact(n.getKey(), new Contact());
  }

  /**
   * Node contacts are IMS managed and the service throws exceptions
   */
  @Test(expected = UnsupportedOperationException.class)
  public void testDeleteContact() {
    Node n = create(newEntity(), 1);
    nodeService.deleteContact(n.getKey(), 1);
  }

  @Test(expected = UnsupportedOperationException.class)
  @Override
  public void testSimpleSearchContact() {
    super.testSimpleSearchContact();
  }


  @Override
  protected Node newEntity() {
    return Nodes.newInstance();
  }

  /**
   * Test doesn't make sense for a node.
   */
  @Override
  public void testCreateAsEditor() {}

  protected Node duplicateForCreateAsEditorTest(Node entity) throws Exception {
    throw new UnsupportedOperationException();
  }

  protected UUID keyForCreateAsEditorTest(Node entity) {
    return null;
  }

  @Test
  public void testSuggest() {
    Node node1 = Nodes.newInstance();
    node1.setTitle("The Node");
    UUID key1 = nodeService.create(node1);

    Node node2 = Nodes.newInstance();
    node2.setTitle("The Great Node");
    UUID key2 = nodeService.create(node2);

    NodeService service = (NodeService) this.getService();
    assertEquals("Should find only The Great Node", 1, service.suggest("Great").size());
    assertEquals("Should find both nodes", 2, service.suggest("the").size());
  }

}
