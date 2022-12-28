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
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.client.DatasetClient;
import org.gbif.registry.ws.client.InstallationClient;
import org.gbif.registry.ws.client.NodeClient;
import org.gbif.registry.ws.client.OrganizationClient;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.util.UUID;

import javax.annotation.Nullable;

import org.junit.jupiter.api.parallel.Execution;
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
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

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

  private void initVotingCountryNodes(ServiceType serviceType) {
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
    n = create(n, serviceType);

    if (TEST_COUNTRIES.containsKey(c)) {
      // create IMS identifiers
      Identifier id = new Identifier();
      id.setType(IdentifierType.GBIF_PARTICIPANT);
      id.setIdentifier(TEST_COUNTRIES.get(c).toString());
      id.setCreatedBy(getSimplePrincipalProvider().get().getName());
      service.addIdentifier(n.getKey(), id);
    }
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  @Execution(CONCURRENT)
  public void testAffiliateNode(ServiceType serviceType) {
    Node n = newEntity(serviceType);
    n.setTitle("GBIF Affiliate Node");
    n.setType(NodeType.OTHER);
    n.setParticipationStatus(ParticipationStatus.AFFILIATE);
    n.setGbifRegion(null);
    n.setCountry(null);
    create(n, serviceType);
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  @Execution(CONCURRENT)
  public void testDatasets(ServiceType serviceType) {
    NodeService service = (NodeService) getService(serviceType);
    OrganizationService organizationService =
        getService(serviceType, organizationResource, organizationClient);
    InstallationService installationService =
        getService(serviceType, installationResource, installationClient);
    DatasetService datasetService = getService(serviceType, datasetResource, datasetClient);

    // endorsing node for the organization
    Node node = create(newEntity(serviceType), serviceType);
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

  /** Node contacts are IMS managed and the service throws exceptions */
  @Override
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  @Execution(CONCURRENT)
  public void testContacts(ServiceType serviceType) {
    NodeService service = (NodeService) getService(serviceType);
    Node n = create(newEntity(serviceType), serviceType);
    assertThrows(UnsupportedOperationException.class, () -> service.listContacts(n.getKey()));
  }

  /** Node contacts are IMS managed and the service throws exceptions */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  @Execution(CONCURRENT)
  public void testAddContact(ServiceType serviceType) {
    NodeService service = ((NodeService) getService(serviceType));
    Node n = create(newEntity(serviceType), serviceType);
    assertThrows(
        UnsupportedOperationException.class, () -> service.addContact(n.getKey(), new Contact()));
  }

  /** Node contacts are IMS managed and the service throws exceptions */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  @Execution(CONCURRENT)
  public void testDeleteContact(ServiceType serviceType) {
    NodeService service = ((NodeService) getService(serviceType));
    Node n = create(newEntity(serviceType), serviceType);
    assertThrows(UnsupportedOperationException.class, () -> service.deleteContact(n.getKey(), 1));
  }

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
}
