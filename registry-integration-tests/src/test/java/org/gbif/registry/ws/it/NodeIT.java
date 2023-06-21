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
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.search.NodeRequestSearchParams;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.util.Range;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.GbifRegion;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.NodeType;
import org.gbif.api.vocabulary.ParticipationStatus;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.client.DatasetClient;
import org.gbif.registry.ws.client.InstallationClient;
import org.gbif.registry.ws.client.NodeClient;
import org.gbif.registry.ws.client.OrganizationClient;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;

import com.google.common.collect.ImmutableMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
public class NodeIT extends NetworkEntityIT<Node> {

  private final OrganizationService organizationResource;
  private final OrganizationService organizationClient;
  private final InstallationService installationResource;
  private final InstallationService installationClient;
  private final DatasetService datasetResource;
  private final DatasetService datasetClient;

  private int count;
  private final TestDataFactory testDataFactory;

  private static final ImmutableMap<Country, Integer> TEST_COUNTRIES =
      ImmutableMap.<Country, Integer>builder()
          .put(Country.AFGHANISTAN, 6)
          .put(Country.ARGENTINA, 16)
          .put(Country.DENMARK, 2)
          .put(Country.SPAIN, 1)
          .build();

  @Autowired
  public NodeIT(
      NodeService nodeService,
      OrganizationService organizationResource,
      InstallationService installationResource,
      DatasetService datasetResource,
      @Nullable SimplePrincipalProvider principalProvider,
      TestDataFactory testDataFactory,
      EsManageServer esServer,
      KeyStore keyStore,
      @LocalServerPort int localServerPort) {
    super(
        nodeService,
        localServerPort,
        keyStore,
        NodeClient.class,
        principalProvider,
        testDataFactory,
        esServer);
    this.organizationResource = organizationResource;
    this.organizationClient = prepareClient(localServerPort, keyStore, OrganizationClient.class);
    this.installationResource = installationResource;
    this.installationClient = prepareClient(localServerPort, keyStore, InstallationClient.class);
    this.datasetResource = datasetResource;
    this.datasetClient = prepareClient(localServerPort, keyStore, DatasetClient.class);
    this.testDataFactory = testDataFactory;
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testGetByCountry(ServiceType serviceType) {
    NodeService service = (NodeService) getService(serviceType);
    initVotingCountryNodes(serviceType);
    insertTestNode(Country.TAIWAN, ParticipationStatus.ASSOCIATE, NodeType.OTHER, serviceType);

    Node n = service.getByCountry(Country.ANGOLA);
    assertNull(n);

    for (Country c : TEST_COUNTRIES.keySet()) {
      n = service.getByCountry(c);
      assertEquals(c, n.getCountry());
    }

    // test taiwan hack
    n = service.getByCountry(Country.TAIWAN);
    assertEquals(Country.TAIWAN, n.getCountry());
  }

  private void initVotingCountryNodes(ServiceType serviceType) {
    count = 0;
    for (Country c : TEST_COUNTRIES.keySet()) {
      insertTestNode(c, ParticipationStatus.VOTING, NodeType.COUNTRY, serviceType);
    }
  }

  private void insertTestNode(
      Country c, ParticipationStatus status, NodeType nodeType, ServiceType serviceType) {
    NodeService service = (NodeService) getService(serviceType);
    Node n = newEntity(serviceType);
    n.setCountry(c);
    n.setTitle("GBIF Node " + c.getTitle());
    n.setType(nodeType);
    n.setParticipationStatus(status);
    n.setGbifRegion(GbifRegion.AFRICA);
    n = create(n, serviceType, count + 1);
    count++;

    if (TEST_COUNTRIES.containsKey(c)) {
      // create Directory identifiers
      Identifier id = new Identifier();
      id.setType(IdentifierType.GBIF_PARTICIPANT);
      id.setIdentifier(TEST_COUNTRIES.get(c).toString());
      id.setCreatedBy(getSimplePrincipalProvider().get().getName());
      service.addIdentifier(n.getKey(), id);
    }
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testAffiliateNode(ServiceType serviceType) {
    Node n = newEntity(serviceType);
    n.setTitle("GBIF Affiliate Node");
    n.setType(NodeType.OTHER);
    n.setParticipationStatus(ParticipationStatus.AFFILIATE);
    n.setGbifRegion(null);
    n.setCountry(null);
    create(n, serviceType, 1);
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testEndorsements(ServiceType serviceType) {
    NodeService service = (NodeService) getService(serviceType);
    OrganizationService organizationService =
        getService(serviceType, organizationResource, organizationClient);

    Node node = testDataFactory.newNode();
    service.create(node);
    node = service.list(new PagingRequest()).getResults().get(0);

    assertResultsOfSize(service.endorsedOrganizations(node.getKey(), new PagingRequest()), 0);
    assertResultsOfSize(service.pendingEndorsements(new PagingRequest()), 0);

    Organization o = testDataFactory.newPersistedOrganization(node.getKey());
    assertResultsOfSize(service.endorsedOrganizations(node.getKey(), new PagingRequest()), 0);
    assertResultsOfSize(service.pendingEndorsements(new PagingRequest()), 1);
    assertResultsOfSize(service.pendingEndorsements(node.getKey(), new PagingRequest()), 1);
    assertEquals(
        1L,
        service.pendingEndorsements(new PagingRequest()).getCount(),
        "Paging is not returning the correct count");

    if (serviceType == ServiceType.RESOURCE) {
      organizationService.confirmEndorsement(o.getKey());
    } else {
      ((OrganizationClient) organizationClient).confirmEndorsementEndpoint(o.getKey());
    }

    assertResultsOfSize(service.pendingEndorsements(new PagingRequest()), 0);
    assertEquals(
        0L,
        service.pendingEndorsements(new PagingRequest()).getCount(),
        "Paging is not returning the correct count");
    assertResultsOfSize(service.endorsedOrganizations(node.getKey(), new PagingRequest()), 1);
    assertEquals(
        1L,
        service.endorsedOrganizations(node.getKey(), new PagingRequest()).getCount(),
        "Paging is not returning the correct count");
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testCountries(ServiceType serviceType) {
    NodeService service = (NodeService) getService(serviceType);
    initVotingCountryNodes(serviceType);
    List<Country> countries = service.listNodeCountries();
    assertEquals(TEST_COUNTRIES.size(), countries.size());
    for (Country c : countries) {
      assertTrue(TEST_COUNTRIES.containsKey(c), "Unexpected node country" + c);
    }
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testActiveCountries(ServiceType serviceType) {
    NodeService service = (NodeService) getService(serviceType);
    initVotingCountryNodes(serviceType);
    List<Country> countries = service.listActiveCountries();
    assertEquals(TEST_COUNTRIES.size() + 1, countries.size());
    for (Country c : countries) {
      assertTrue(
          Country.TAIWAN == c || TEST_COUNTRIES.containsKey(c), "Unexpected node country" + c);
    }
    assertTrue(countries.contains(Country.TAIWAN), "Taiwan missing");

    // insert extra observer nodes and make sure we get the same list
    insertTestNode(Country.BOTSWANA, ParticipationStatus.OBSERVER, NodeType.COUNTRY, serviceType);
    insertTestNode(Country.HONG_KONG, ParticipationStatus.FORMER, NodeType.COUNTRY, serviceType);

    List<Country> countries2 = service.listActiveCountries();
    assertEquals(countries, countries2);
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testDatasets(ServiceType serviceType) {
    NodeService service = (NodeService) getService(serviceType);
    OrganizationService organizationService =
        getService(serviceType, organizationResource, organizationClient);
    InstallationService installationService =
        getService(serviceType, installationResource, installationClient);
    DatasetService datasetService = getService(serviceType, datasetResource, datasetClient);

    // endorsing node for the organization
    Node node = create(newEntity(serviceType), serviceType, 1);
    // publishing organization (required field)
    Organization o = testDataFactory.newOrganization(node.getKey());
    o.setEndorsingNodeKey(node.getKey());
    UUID organizationKey = organizationService.create(o);

    // endorse organization
    if (serviceType == ServiceType.RESOURCE) {
      organizationService.confirmEndorsement(organizationKey);
    } else {
      ((OrganizationClient) organizationClient).confirmEndorsementEndpoint(organizationKey);
    }

    // hosting technical installation (required field)
    Installation i = testDataFactory.newInstallation(organizationKey);
    UUID installationKey = installationService.create(i);
    // 2 datasets
    Dataset d1 = testDataFactory.newDataset(organizationKey, installationKey);
    datasetService.create(d1);
    Dataset d2 = testDataFactory.newDataset(organizationKey, installationKey);
    UUID d2Key = datasetService.create(d2);

    // test node service
    PagingResponse<Dataset> resp = service.endorsedDatasets(node.getKey(), new PagingRequest());
    assertEquals(2, resp.getResults().size());
    assertEquals(2L, resp.getCount(), "Paging is not returning the correct count");

    // the last created dataset should be the first in the list
    assertEquals(d2Key, resp.getResults().get(0).getKey());
  }

  /**
   * A test that requires a configured Directory with real spanish data. Jenkins is configured for
   * this, so we activate this test to make sure Directory connections are working!
   */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testDirectory(ServiceType serviceType) {
    NodeService service = (NodeService) getService(serviceType);
    initVotingCountryNodes(serviceType);
    Node es = service.getByCountry(Country.SPAIN);
    assertEquals((Integer) 2001, es.getParticipantSince());
    assertEquals(ParticipationStatus.VOTING, es.getParticipationStatus());
    // this is no real data, it comes from the test inserts above
    assertEquals(GbifRegion.AFRICA, es.getGbifRegion());
    assertEquals("GBIF.ES", es.getAbbreviation());
    assertEquals("Madrid", es.getCity());
    assertEquals("E-28014", es.getPostalCode());
    assertEquals("Real Jardín Botánico - CSIC", es.getOrganization());
    assertTrue(es.getContacts().size() > 5);

    Node notInDirectory = service.getByCountry(Country.AFGHANISTAN);
    assertNotNull(notInDirectory);
  }

  /** Node contacts are Directory managed and the service throws exceptions */
  @Override
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testContacts(ServiceType serviceType) {
    NodeService service = (NodeService) getService(serviceType);
    Node n = create(newEntity(serviceType), serviceType, 1);
    assertThrows(UnsupportedOperationException.class, () -> service.listContacts(n.getKey()));
  }

  /** Node contacts are Directory managed and the service throws exceptions */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testAddContact(ServiceType serviceType) {
    NodeService service = ((NodeService) getService(serviceType));
    Node n = create(newEntity(serviceType), serviceType, 1);
    assertThrows(
        UnsupportedOperationException.class, () -> service.addContact(n.getKey(), new Contact()));
  }

  /** Node contacts are Directory managed and the service throws exceptions */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testDeleteContact(ServiceType serviceType) {
    NodeService service = ((NodeService) getService(serviceType));
    Node n = create(newEntity(serviceType), serviceType, 1);
    assertThrows(UnsupportedOperationException.class, () -> service.deleteContact(n.getKey(), 1));
  }

  @Override
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testSimpleSearchContact(ServiceType serviceType) {
    assertThrows(
        UnsupportedOperationException.class, () -> super.testSimpleSearchContact(serviceType));
  }

  // TODO: list test, create it in the base class??

  @Override
  protected Node newEntity(ServiceType serviceType) {
    return testDataFactory.newNode();
  }

  @Override
  protected Node duplicateForCreateAsEditorTest(Node entity) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected UUID keyForCreateAsEditorTest(Node entity) {
    return null;
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testSuggest(ServiceType serviceType) {
    NodeService service = (NodeService) getService(serviceType);
    Node node1 = testDataFactory.newNode();
    node1.setTitle("The Node");
    service.create(node1);

    Node node2 = testDataFactory.newNode();
    node2.setTitle("The Great Node");
    service.create(node2);

    Node node3 = testDataFactory.newNode();
    node3.setTitle("The Great Node 3");
    UUID key3 = service.create(node3);

    assertEquals(2, service.suggest("Great").size(), "Should find only the 2 The Great Node");
    assertEquals(3, service.suggest("the").size(), "Should find all nodes");

    service.delete(key3);
    assertEquals(1, service.suggest("Great").size(), "Should find only The Great Node");
    assertEquals(2, service.suggest("the").size(), "Should find both nodes");
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testList(ServiceType serviceType) {
    NodeService service = (NodeService) getService(serviceType);

    Node n1 = newEntity(serviceType);
    n1.setTitle("first node");
    UUID key1 = getService(serviceType).create(n1);

    Identifier id1 = newTestIdentifier(n1, IdentifierType.DOI, "doi:1");
    service.addIdentifier(key1, id1);
    MachineTag mt1 = new MachineTag("ns", "mt1", "mtV1");
    service.addMachineTag(key1, mt1);

    Node n2 = newEntity(serviceType);
    n2.setTitle("second node");
    UUID key2 = getService(serviceType).create(n2);

    assertResultsOfSize(service.list(new NodeRequestSearchParams()), 2);

    NodeRequestSearchParams searchParams = new NodeRequestSearchParams();
    searchParams.setQ("node");
    assertResultsOfSize(service.list(searchParams), 2);
    searchParams.setQ("second");
    assertResultsOfSize(service.list(searchParams), 1);
    searchParams.setQ("third");
    assertResultsOfSize(service.list(searchParams), 0);

    searchParams = new NodeRequestSearchParams();
    searchParams.setIdentifierType(id1.getType());
    assertResultsOfSize(service.list(searchParams), 1);

    searchParams = new NodeRequestSearchParams();
    searchParams.setIdentifier(id1.getIdentifier());
    assertResultsOfSize(service.list(searchParams), 1);

    searchParams = new NodeRequestSearchParams();
    searchParams.setMachineTagName(mt1.getName());
    assertResultsOfSize(service.list(searchParams), 1);

    searchParams = new NodeRequestSearchParams();
    searchParams.setIdentifier(id1.getIdentifier());
    searchParams.setMachineTagNamespace(mt1.getNamespace());
    assertResultsOfSize(service.list(searchParams), 1);

    searchParams = new NodeRequestSearchParams();
    searchParams.setModified(
        Range.closed(LocalDate.now(), LocalDate.now().plus(1, ChronoUnit.DAYS)));
    assertResultsOfSize(service.list(searchParams), 2);

    searchParams.setModified(
        Range.closed(
            LocalDate.now().minus(2, ChronoUnit.MONTHS),
            LocalDate.now().minus(1, ChronoUnit.MONTHS)));
    assertResultsOfSize(service.list(searchParams), 0);
  }
}
