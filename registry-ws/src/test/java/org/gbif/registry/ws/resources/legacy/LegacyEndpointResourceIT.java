package org.gbif.registry.ws.resources.legacy;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.utils.Datasets;
import org.gbif.registry.utils.Endpoints;
import org.gbif.registry.utils.Installations;
import org.gbif.registry.utils.Organizations;
import org.gbif.registry.utils.Parsers;
import org.gbif.registry.utils.Requests;
import org.gbif.registry.ws.util.LegacyResourceConstants;
import org.gbif.utils.HttpUtil;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;

import com.google.inject.Injector;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.SAXException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LegacyEndpointResourceIT {

  // Flushes the database on each run
  @ClassRule
  public static final LiquibaseInitializer liquibaseRule = new LiquibaseInitializer(RegistryTestModules.database());

  @ClassRule
  public static final RegistryServer registryServer = RegistryServer.INSTANCE;

  @Rule
  public final DatabaseInitializer databaseRule = new DatabaseInitializer(RegistryTestModules.database());

  public static final String ENDPOINT_DESCRIPTION = "Description of Test Endpoint";
  public static final String ENDPOINT_TYPE = "EML";
  public static final String ENDPOINT_ACCESS_POINT_URL = "http://ipt.gbif.org/eml.do?r=bigdbtest&v=18";

  private final DatasetService datasetService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public LegacyEndpointResourceIT() throws ParserConfigurationException, SAXException {
    Injector i = RegistryTestModules.webservice();
    this.datasetService = i.getInstance(DatasetService.class);
  }

  /**
   * The test begins by persisting a new Organization, Installation associated to the Organization, and Dataset
   * associated to the Organization.
   * </br>
   * Then, it sends a create Endpoint (POST) request to create a new Endpoint associated to this dataset.
   * </br>
   * Upon receiving an HTTP Response, the test parses its XML content in order to extract the created Endpoint key.
   * The content is parsed exactly the same way as the GBRDS WS consumer would do it.
   * </br>
   * Last, the test validates that the endpoint was persisted correctly.
   */
  @Test
  public void testRegisterLegacyEndpoint() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // persist new installation
    Installation installation = Installations.newPersistedInstance(organizationKey);
    UUID installationKey = installation.getKey();

    // persist new Dataset associated to installation
    Dataset dataset = Datasets.newPersistedInstance(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();

    // populate params for ws
    List<NameValuePair> data = buildLegacyEndpointParameters(datasetKey);
    UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(data, Charset.forName("UTF-8"));

    // construct request uri
    String uri = Requests.getRequestUri("/registry/service");

    // before sending the update POST request, count the number of endpoints
    assertEquals(0, datasetService.listEndpoints(datasetKey).size());

    // send POST request with credentials
    HttpUtil.Response result = Requests.http.post(uri, null, null, Organizations.credentials(organization), uefe);

    // parse newly registered endpoint key (id)
    Parsers.saxParser.parse(Parsers.getUtf8Stream(result.content), Parsers.legacyEndpointResponseHandler);
    assertNotNull("Registered Endpoint key should be in response", Parsers.legacyEndpointResponseHandler.key);
    assertEquals(datasetKey.toString(), Parsers.legacyEndpointResponseHandler.resourceKey);
    assertEquals(ENDPOINT_ACCESS_POINT_URL, Parsers.legacyEndpointResponseHandler.accessPointURL);
    assertEquals(ENDPOINT_TYPE, Parsers.legacyEndpointResponseHandler.type);
    assertEquals(ENDPOINT_DESCRIPTION, Parsers.legacyEndpointResponseHandler.description);

    // count the number of endpoints
    assertEquals(1, datasetService.listEndpoints(datasetKey).size());
  }

  /**
   * The test sends a create Endpoint (POST) request to create a new Dataset Endpoint, however, its credentials are
   * not authorized. The test must check that the server responds  with a 401 Unauthorized Response.
   */
  @Test
  public void testRegisterLegacyEndpointButNotAuthorized() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // persist new installation
    Installation installation = Installations.newPersistedInstance(organizationKey);
    UUID installationKey = installation.getKey();

    // persist new Dataset associated to installation
    Dataset dataset = Datasets.newPersistedInstance(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();

    // populate params for ws
    List<NameValuePair> data = buildLegacyEndpointParameters(datasetKey);
    UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(data, Charset.forName("UTF-8"));

    // construct request uri
    String uri = Requests.getRequestUri("/registry/service");

    // send POST request with credentials
    // assign the organization the random generated key, to provoke authorization failure
    organization.setKey(UUID.randomUUID());
    HttpUtil.Response result = Requests.http.post(uri, null, null, Organizations.credentials(organization), uefe);

    // 401 expected
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), result.getStatusCode());
  }

  /**
   * The test sends a create Endpoint (POST) request to create a new Dataset Endpoint, however, its type HTTP parameter
   * is missing. The test must check that the server responds  with a 400 BAD_REQUEST Response.
   */
  @Test
  public void testRegisterLegacyEndpointWithNoType() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // persist new installation
    Installation installation = Installations.newPersistedInstance(organizationKey);
    UUID installationKey = installation.getKey();

    // persist new Dataset associated to installation
    Dataset dataset = Datasets.newPersistedInstance(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();

    // populate params for ws
    List<NameValuePair> data = buildLegacyEndpointParameters(datasetKey);
    assertEquals(4, data.size());
    // remove mandatory key/value before sending
    Iterator<NameValuePair> iter = data.iterator();
    while (iter.hasNext()) {
      NameValuePair pair = iter.next();
      if (pair.getName().equals("type")) {
        iter.remove();
      }
    }
    assertEquals(3, data.size());
    UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(data, Charset.forName("UTF-8"));

    // construct request uri
    String uri = Requests.getRequestUri("/registry/service");

    // send POST request with credentials
    HttpUtil.Response result = Requests.http.post(uri, null, null, Organizations.credentials(organization), uefe);

    // 400 expected
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), result.getStatusCode());
  }

  /**
   * The test begins by persisting a new Organization, Installation associated to the Organization, and Dataset
   * associated to the Organization. Then it adds an Endpoints for the Dataset.
   * </br>
   * Then, it sends a delete all Endpoints (DELETE) request to delete all the Endpoints associated to this dataset.
   * </br>
   * Last, the test validates that the endpoints were deleted correctly.
   */
  @Test
  public void testDeleteAllDatasetEndpoints() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // persist new installation
    Installation installation = Installations.newPersistedInstance(organizationKey);
    UUID installationKey = installation.getKey();

    // persist new Dataset associated to installation
    Dataset dataset = Datasets.newPersistedInstance(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();

    // add endpooint for Dataset
    Endpoint endpoint = Endpoints.newInstance();
    datasetService.addEndpoint(datasetKey, endpoint);

    // count the number of endpoints
    assertEquals(1, datasetService.listEndpoints(datasetKey).size());

    // construct update request uri
    String uri = Requests.getRequestUri("/registry/service?resourceKey=" + datasetKey.toString());

    // send delete POST request
    HttpUtil.Response result = Requests.http.delete(uri, Organizations.credentials(organization));

    // check that the dataset was deleted
    assertEquals(200, result.getStatusCode());
    assertEquals(0, datasetService.listEndpoints(datasetKey).size());
  }

  /**
   * The test sends a get all endpoints for dataset (GET) request, the JSON response having at the very least
   * the dataset key, endpoint key, endpoint type, and endpoint url.
   */
  @Test
  public void testGetLegacyEndpointsForDatasetJSON() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // persist new installation of type IPT
    Installation installation = Installations.newPersistedInstance(organizationKey);
    UUID installationKey = installation.getKey();

    // persist new Dataset associated to installation
    Dataset dataset = Datasets.newPersistedInstance(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();

    // add endpooint for Dataset
    Endpoint endpoint = Endpoints.newInstance();
    int endpointKey = datasetService.addEndpoint(datasetKey, endpoint);

    // count the number of endpoints
    assertEquals(1, datasetService.listEndpoints(datasetKey).size());

    // construct request uri
    String uri = Requests.getRequestUri("/registry/service.json?resourceKey=" + datasetKey.toString());

    // send GET request with no credentials
    HttpUtil.Response result = Requests.http.get(uri);

    // JSON array expected, with single endpoint
    assertTrue(result.content.startsWith("[") && result.content.endsWith("]"));
    JsonNode rootNode = objectMapper.readTree(result.content);
    assertEquals(1, rootNode.size());
    // all the expected keys in response?
    assertEquals(String.valueOf(endpointKey), rootNode.get(0).get(LegacyResourceConstants.KEY_PARAM).getTextValue());
    assertEquals(datasetKey.toString(), rootNode.get(0).get(LegacyResourceConstants.RESOURCE_KEY_PARAM).getTextValue());
    assertEquals(endpoint.getType().toString(), rootNode.get(0).get(LegacyResourceConstants.TYPE_PARAM).getTextValue());
    assertEquals(endpoint.getUrl().toASCIIString(),
                 rootNode.get(0).get(LegacyResourceConstants.ACCESS_POINT_URL_PARAM).getTextValue());
    // simply empty values, but expected nonetheless
    assertNotNull(rootNode.get(0).get(LegacyResourceConstants.DESCRIPTION_LANGUAGE_PARAM).getTextValue());
    assertNotNull(rootNode.get(0).get(LegacyResourceConstants.TYPE_DESCRIPTION_PARAM).getTextValue());
  }

  /**
   * The test sends a get all endpoints for dataset (GET) request, the XML response having at the very least
   * the dataset key, endpoint key, endpoint type, and endpoint url.
   */
  @Test
  public void testGetLegacyEndpointsForDatasetXML() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // persist new installation of type IPT
    Installation installation = Installations.newPersistedInstance(organizationKey);
    UUID installationKey = installation.getKey();

    // persist new Dataset associated to installation
    Dataset dataset = Datasets.newPersistedInstance(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();

    // add endpooint for Dataset
    Endpoint endpoint = Endpoints.newInstance();
    int endpointKey = datasetService.addEndpoint(datasetKey, endpoint);

    // count the number of endpoints
    assertEquals(1, datasetService.listEndpoints(datasetKey).size());

    // construct request uri
    String uri = Requests.getRequestUri("/registry/service?resourceKey=" + datasetKey.toString());

    // send GET request with no credentials
    HttpUtil.Response result = Requests.http.get(uri);

    // TODO: Response must be wrapped with root <services>, not <legacyEndpointResponses>
    assertTrue(result.content.contains("<legacyEndpointResponses><service>"));

    // parse returned list of services
    Parsers.saxParser.parse(Parsers.getUtf8Stream(result.content), Parsers.legacyEndpointResponseHandler);
    assertEquals(String.valueOf(endpointKey), Parsers.legacyEndpointResponseHandler.key);
    assertEquals(datasetKey.toString(), Parsers.legacyEndpointResponseHandler.resourceKey);
    assertEquals(endpoint.getType().name(), Parsers.legacyEndpointResponseHandler.type);
    assertEquals(endpoint.getUrl().toASCIIString(), Parsers.legacyEndpointResponseHandler.accessPointURL);
    assertEquals(endpoint.getDescription(), Parsers.legacyEndpointResponseHandler.description);
    assertEquals("", Parsers.legacyEndpointResponseHandler.organisationKey);
    assertEquals("", Parsers.legacyEndpointResponseHandler.typeDescription);
    assertEquals("", Parsers.legacyEndpointResponseHandler.descriptionLanguage);
  }

  /**
   * The test sends a get endpoint for dataset (GET) request for a dataset that does not exist.
   * The JSON response having an error message, not a 404.
   */
  @Test
  public void testGetLegacyEndpointsForDatasetNotFoundJSON() throws Exception {
    // construct request uri
    String uri = Requests.getRequestUri("/registry/service.json?resourceKey=" + UUID.randomUUID().toString());

    // send GET request with no credentials
    HttpUtil.Response result = Requests.http.get(uri);

    // JSON object expected, representing single dataset
    assertTrue(result.content.equalsIgnoreCase("{\"error\":\"No dataset matches the key provided\"}"));
  }

  /**
   * Populate a list of name value pairs used in the common ws requests for GBRDS endpoint creation.
   *
   * @param datasetKey dataset key
   *
   * @return list of name value pairs
   */
  private List<NameValuePair> buildLegacyEndpointParameters(UUID datasetKey) {
    List<NameValuePair> data = new ArrayList<NameValuePair>();
    data.add(new BasicNameValuePair(LegacyResourceConstants.RESOURCE_KEY_PARAM, datasetKey.toString()));
    data.add(new BasicNameValuePair(LegacyResourceConstants.DESCRIPTION_PARAM, ENDPOINT_DESCRIPTION));
    data.add(new BasicNameValuePair(LegacyResourceConstants.TYPE_PARAM, ENDPOINT_TYPE));
    data.add(new BasicNameValuePair(LegacyResourceConstants.ACCESS_POINT_URL_PARAM, ENDPOINT_ACCESS_POINT_URL));
    return data;
  }

  /**
   * The test sends a get all service types (GET) request, the JSON response having a name, description,
   * overviewURL, and key for each service in the list.
   */
  @Test
  public void testGetServiceTypes() throws Exception {

    // construct request uri
    String uri = Requests.getRequestUri("/registry/service.json?op=types");

    // send GET request with no credentials
    HttpUtil.Response result = Requests.http.get(uri);

    JsonNode rootNode = objectMapper.readTree(result.content);

    // JSON object expected, with array of services [{},{},..]
    assertTrue(result.content.startsWith("[") && result.content.endsWith("]"));
    JsonNode node0 = rootNode.get(0);
    assertEquals("EML", node0.get("name").asText());
  }
}
