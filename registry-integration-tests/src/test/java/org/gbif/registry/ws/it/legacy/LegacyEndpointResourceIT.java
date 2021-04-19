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
package org.gbif.registry.ws.it.legacy;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.domain.ws.ErrorResponse;
import org.gbif.registry.domain.ws.LegacyEndpointResponse;
import org.gbif.registry.domain.ws.LegacyEndpointResponseListWrapper;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.registry.ws.it.fixtures.RequestTestFixture;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.ACCESS_POINT_URL_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.DESCRIPTION_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.RESOURCE_KEY_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.TYPE_PARAM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class LegacyEndpointResourceIT extends BaseItTest {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = TestCaseDatabaseInitializer.builder()
    .dataSource(database.getTestDatabase())
    .build();

  public static final String ENDPOINT_DESCRIPTION = "Description of Test Endpoint";
  public static final String ENDPOINT_TYPE = "EML";
  public static final String ENDPOINT_ACCESS_POINT_URL =
      "http://ipt.gbif.org/eml.do?r=bigdbtest&v=18";

  private final RequestTestFixture requestTestFixture;
  private final DatasetService datasetService;
  private final TestDataFactory testDataFactory;

  @Autowired
  public LegacyEndpointResourceIT(
      RequestTestFixture requestTestFixture,
      DatasetService datasetService,
      TestDataFactory testDataFactory,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer) {
    super(principalProvider, esServer);
    this.requestTestFixture = requestTestFixture;
    this.datasetService = datasetService;
    this.testDataFactory = testDataFactory;
  }

  /**
   * The test begins by persisting a new Organization, Installation associated to the Organization,
   * and Dataset associated to the Organization. </br> Then, it sends a create Endpoint (POST)
   * request to create a new Endpoint associated to this dataset. </br> Upon receiving an HTTP
   * Response, the test parses its XML content in order to extract the created Endpoint key. The
   * content is parsed exactly the same way as the GBRDS WS consumer would do it. </br> Last, the
   * test validates that the endpoint was persisted correctly.
   */
  @Test
  public void testRegisterLegacyEndpoint() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    UUID organizationKey = organization.getKey();
    assertNotNull(organizationKey);

    // persist new installation
    Installation installation = testDataFactory.newPersistedInstallation(organizationKey);
    UUID installationKey = installation.getKey();
    assertNotNull(installationKey);

    // persist new Dataset associated to installation
    Dataset dataset = testDataFactory.newPersistedDataset(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();
    assertNotNull(datasetKey);

    // populate params for ws
    MultiValueMap<String, String> data = buildLegacyEndpointParameters(datasetKey);

    // construct request uri
    String uri = "/registry/service";

    // before sending the update POST request, count the number of endpoints
    assertEquals(0, datasetService.listEndpoints(datasetKey).size());

    // send POST request with credentials
    ResultActions actions =
        requestTestFixture
            .postRequestUrlEncoded(data, organizationKey, organization.getPassword(), uri)
            .andExpect(status().is2xxSuccessful());

    // parse newly registered endpoint key (id)
    LegacyEndpointResponse response =
        requestTestFixture.extractXmlResponse(actions, LegacyEndpointResponse.class);
    assertNotNull(response.getKey(), "Registered Endpoint key should be in response");
    assertEquals(datasetKey.toString(), response.getResourceKey());
    assertEquals(ENDPOINT_ACCESS_POINT_URL, response.getAccessPointURL());
    assertEquals(ENDPOINT_TYPE, response.getType());
    assertEquals(ENDPOINT_DESCRIPTION, response.getDescription());

    // count the number of endpoints
    assertEquals(1, datasetService.listEndpoints(datasetKey).size());
  }

  /**
   * The test sends a create Endpoint (POST) request to create a new Dataset Endpoint, however, its
   * credentials are not authorized. The test must check that the server responds with a 401
   * Unauthorized Response.
   */
  @Test
  public void testRegisterLegacyEndpointButNotAuthorized() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    UUID organizationKey = organization.getKey();
    assertNotNull(organizationKey);

    // persist new installation
    Installation installation = testDataFactory.newPersistedInstallation(organizationKey);
    UUID installationKey = installation.getKey();
    assertNotNull(installationKey);

    // persist new Dataset associated to installation
    Dataset dataset = testDataFactory.newPersistedDataset(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();
    assertNotNull(datasetKey);

    // populate params for ws
    MultiValueMap<String, String> data = buildLegacyEndpointParameters(datasetKey);

    // construct request uri
    String uri = "/registry/service";

    // send POST request with credentials
    // assign the organization the random generated key, to provoke authorization failure
    // 401 expected
    requestTestFixture
        .postRequestUrlEncoded(data, UUID.randomUUID(), organization.getPassword(), uri)
        .andExpect(status().isUnauthorized());
  }

  /**
   * The test sends a create Endpoint (POST) request to create a new Dataset Endpoint, however, its
   * type HTTP parameter is missing. The test must check that the server responds with a 400
   * BAD_REQUEST Response.
   */
  @Test
  public void testRegisterLegacyEndpointWithNoType() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    UUID organizationKey = organization.getKey();
    assertNotNull(organizationKey);

    // persist new installation
    Installation installation = testDataFactory.newPersistedInstallation(organizationKey);
    UUID installationKey = installation.getKey();
    assertNotNull(installationKey);

    // persist new Dataset associated to installation
    Dataset dataset = testDataFactory.newPersistedDataset(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();
    assertNotNull(datasetKey);

    // populate params for ws
    MultiValueMap<String, String> data = buildLegacyEndpointParameters(datasetKey);
    assertEquals(4, data.size());
    // remove mandatory key/value before sending
    data.remove(TYPE_PARAM);
    assertEquals(3, data.size());

    // construct request uri
    String uri = "/registry/service";

    // send POST request with credentials
    // 400 expected
    requestTestFixture
        .postRequestUrlEncoded(data, organizationKey, organization.getPassword(), uri)
        .andExpect(status().isBadRequest());
  }

  /**
   * The test begins by persisting a new Organization, Installation associated to the Organization,
   * and Dataset associated to the Organization. Then it adds an Endpoints for the Dataset. </br>
   * Then, it sends a delete all Endpoints (DELETE) request to delete all the Endpoints associated
   * to this dataset. </br> Last, the test validates that the endpoints were deleted correctly.
   */
  @Test
  public void testDeleteAllDatasetEndpoints() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    UUID organizationKey = organization.getKey();
    assertNotNull(organizationKey);

    // persist new installation
    Installation installation = testDataFactory.newPersistedInstallation(organizationKey);
    UUID installationKey = installation.getKey();
    assertNotNull(installationKey);

    // persist new Dataset associated to installation
    Dataset dataset = testDataFactory.newPersistedDataset(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();
    assertNotNull(datasetKey);

    // add endpooint for Dataset
    Endpoint endpoint = testDataFactory.newEndpoint();
    datasetService.addEndpoint(datasetKey, endpoint);

    // count the number of endpoints
    assertEquals(1, datasetService.listEndpoints(datasetKey).size());

    // construct update request uri
    String uri = "/registry/service?resourceKey=" + datasetKey;

    // send delete POST request
    requestTestFixture
        .deleteRequestUrlEncoded(organizationKey, organization.getPassword(), uri)
        .andExpect(status().isOk());

    // check that the dataset was deleted
    assertEquals(0, datasetService.listEndpoints(datasetKey).size());
  }

  /**
   * The test sends a get all endpoints for dataset (GET) request, the JSON response having at the
   * very least the dataset key, endpoint key, endpoint type, and endpoint url.
   */
  @Test
  public void testGetLegacyEndpointsForDatasetJSON() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    UUID organizationKey = organization.getKey();
    assertNotNull(organizationKey);

    // persist new installation of type IPT
    Installation installation = testDataFactory.newPersistedInstallation(organizationKey);
    UUID installationKey = installation.getKey();
    assertNotNull(installationKey);

    // persist new Dataset associated to installation
    Dataset dataset = testDataFactory.newPersistedDataset(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();
    assertNotNull(datasetKey);

    // add endpooint for Dataset
    Endpoint endpoint = testDataFactory.newEndpoint();
    int endpointKey = datasetService.addEndpoint(datasetKey, endpoint);

    // count the number of endpoints
    assertEquals(1, datasetService.listEndpoints(datasetKey).size());

    // construct request uri
    String uri = "/registry/service.json?resourceKey=" + datasetKey;

    // send GET request with no credentials
    ResultActions actions =
        requestTestFixture.getRequest(uri).andExpect(status().is2xxSuccessful());

    LegacyEndpointResponseListWrapper responseWrapper =
        requestTestFixture.extractJsonResponse(actions, LegacyEndpointResponseListWrapper.class);

    // JSON array expected, with single endpoint
    LegacyEndpointResponse response = responseWrapper.getLegacyEndpointResponses().get(0);
    assertEquals(1, responseWrapper.getLegacyEndpointResponses().size());
    // all the expected keys in response?
    assertEquals(String.valueOf(endpointKey), response.getKey());
    assertEquals(datasetKey.toString(), response.getResourceKey());
    assertEquals(endpoint.getType().toString(), response.getType());
    assertNotNull(endpoint.getUrl());
    assertEquals(endpoint.getUrl().toASCIIString(), response.getAccessPointURL());
    // simply empty values, but expected nonetheless
    assertNotNull(response.getDescriptionLanguage());
    assertNotNull(response.getTypeDescription());
  }

  /**
   * The test sends a get all endpoints for dataset (GET) request, the XML response having at the
   * very least the dataset key, endpoint key, endpoint type, and endpoint url.
   */
  @Test
  public void testGetLegacyEndpointsForDatasetXML() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    UUID organizationKey = organization.getKey();
    assertNotNull(organizationKey);

    // persist new installation of type IPT
    Installation installation = testDataFactory.newPersistedInstallation(organizationKey);
    UUID installationKey = installation.getKey();
    assertNotNull(installationKey);

    // persist new Dataset associated to installation
    Dataset dataset = testDataFactory.newPersistedDataset(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();
    assertNotNull(datasetKey);

    // add endpooint for Dataset
    Endpoint endpoint = testDataFactory.newEndpoint();
    int endpointKey = datasetService.addEndpoint(datasetKey, endpoint);

    // count the number of endpoints
    assertEquals(1, datasetService.listEndpoints(datasetKey).size());

    // construct request uri
    String uri = "/registry/service?resourceKey=" + datasetKey;

    // send GET request with no credentials
    ResultActions actions =
        requestTestFixture.getRequest(uri).andExpect(status().is2xxSuccessful());

    // parse returned list of services
    LegacyEndpointResponseListWrapper responseWrapper =
        requestTestFixture.extractXmlResponse(actions, LegacyEndpointResponseListWrapper.class);
    LegacyEndpointResponse response = responseWrapper.getLegacyEndpointResponses().get(0);

    assertEquals(String.valueOf(endpointKey), response.getKey());
    assertEquals(datasetKey.toString(), response.getResourceKey());
    assertEquals(endpoint.getType().toString(), response.getType());
    assertNotNull(endpoint.getUrl());
    assertEquals(endpoint.getUrl().toASCIIString(), response.getAccessPointURL());
    assertEquals(endpoint.getDescription(), response.getDescription());
    assertEquals("", response.getOrganisationKey());
    assertEquals("", response.getTypeDescription());
    assertEquals("", response.getDescriptionLanguage());
  }

  /**
   * The test sends a get endpoint for dataset (GET) request for a dataset that does not exist. The
   * JSON response having an error message, not a 404.
   */
  @Test
  public void testGetLegacyEndpointsForDatasetNotFoundJSON() throws Exception {
    // construct request uri
    String uri = "/registry/service.json?resourceKey=" + UUID.randomUUID();

    // send GET request with no credentials
    ResultActions actions =
        requestTestFixture.getRequest(uri).andExpect(status().is2xxSuccessful());

    ErrorResponse response = requestTestFixture.extractJsonResponse(actions, ErrorResponse.class);
    // JSON object expected, representing single dataset
    assertEquals("No dataset matches the key provided", response.getError());
  }

  /**
   * Populate a list of name value pairs used in the common ws requests for GBRDS endpoint creation.
   *
   * @param datasetKey dataset key
   * @return list of name value pairs
   */
  private MultiValueMap<String, String> buildLegacyEndpointParameters(UUID datasetKey) {
    MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
    data.add(RESOURCE_KEY_PARAM, datasetKey.toString());
    data.add(DESCRIPTION_PARAM, ENDPOINT_DESCRIPTION);
    data.add(TYPE_PARAM, ENDPOINT_TYPE);
    data.add(ACCESS_POINT_URL_PARAM, ENDPOINT_ACCESS_POINT_URL);
    return data;
  }

  /**
   * The test sends a get all service types (GET) request, the JSON response having a name,
   * description, overviewURL, and key for each service in the list.
   */
  @Test
  public void testGetServiceTypes() throws Exception {

    // construct request uri
    String uri = "/registry/service.json?op=types";

    // send GET request with no credentials
    ResultActions actions =
        requestTestFixture.getRequest(uri).andExpect(status().is2xxSuccessful());

    String response = requestTestFixture.extractResponse(actions);
    assertTrue(response.contains("EML"));
  }
}
