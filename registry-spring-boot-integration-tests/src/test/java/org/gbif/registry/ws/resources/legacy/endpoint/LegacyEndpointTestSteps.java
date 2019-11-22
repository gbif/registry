package org.gbif.registry.ws.resources.legacy.endpoint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.ws.TestEmailConfiguration;
import org.gbif.registry.ws.model.LegacyEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.gbif.registry.utils.LenientAssert.assertLenientEquals;
import static org.gbif.registry.ws.util.LegacyResourceConstants.ACCESS_POINT_URL_PARAM;
import static org.gbif.registry.ws.util.LegacyResourceConstants.DESCRIPTION_PARAM;
import static org.gbif.registry.ws.util.LegacyResourceConstants.RESOURCE_KEY_PARAM;
import static org.gbif.registry.ws.util.LegacyResourceConstants.TYPE_PARAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {TestEmailConfiguration.class,
  RegistryIntegrationTestsConfiguration.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class LegacyEndpointTestSteps {

  private MockMvc mvc;
  private ResultActions result;
  private HttpHeaders requestParamsEndpoint;
  private Endpoint actualEndpoint;
  private UUID datasetKey;

  @Autowired
  private DatasetService datasetService;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private DataSource ds;

  private Connection connection;

  @Autowired
  private XmlMapper xmlMapper;

  @Autowired
  private ObjectMapper objectMapper;

  @Before("@LegacyEndpoint")
  public void setUp() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/legacyendpoint/legacy_endpoint_cleanup.sql"));
    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/legacyendpoint/legacy_endpoint_prepare.sql"));

    mvc = MockMvcBuilders
      .webAppContextSetup(context)
      .apply(springSecurity())
      .build();
  }

  @After("@LegacyEndpoint")
  public void tearDown() throws Exception {
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/legacyendpoint/legacy_endpoint_cleanup.sql"));

    connection.close();
  }

  @Given("organization {string} with key {string}")
  public void prepareOrganization(String orgName, String orgKey) {
    // prepared by script, see @Before
  }

  @Given("installation {string} with key {string}")
  public void prepareInstallation(String instName, String installationKey) {
    // prepared by script, see @Before
  }

  @Given("dataset {string} with key {string}")
  public void prepareDataset(String name, String datasetKey) {
    this.datasetKey = UUID.fromString(datasetKey);
  }

  @Given("endpoint request parameters")
  public void prepareRequestParamsEndpoint(Map<String, String> params) {
    requestParamsEndpoint = new HttpHeaders();
    requestParamsEndpoint.add(RESOURCE_KEY_PARAM, params.get(RESOURCE_KEY_PARAM));
    requestParamsEndpoint.add(DESCRIPTION_PARAM, params.get(DESCRIPTION_PARAM));
    requestParamsEndpoint.add(TYPE_PARAM, params.get(TYPE_PARAM));
    requestParamsEndpoint.add(ACCESS_POINT_URL_PARAM, params.get(ACCESS_POINT_URL_PARAM));
  }

  @Given("{int} endpoint(s) in database before/after")
  public void checkNumberOfEndpoints(int expectedNumber) {
    assertEquals(expectedNumber, datasetService.listEndpoints(datasetKey).size());
  }

  @Given("exclude parameter {string} from endpoint request parameters")
  public void removeFromParams(String field) {
    requestParamsEndpoint.remove(field);
  }

  @When("register new endpoint using valid/invalid organization key {string} and password {string}")
  public void registerEndpoint(String organisationKey, String password) throws Exception {
    result = mvc
      .perform(
        post("/registry/service")
          .params(requestParamsEndpoint)
          .contentType(APPLICATION_FORM_URLENCODED)
          .accept(APPLICATION_XML)
          .with(httpBasic(organisationKey, password)));
  }

  @When("delete endpoint by (in)valid resource key {string} using valid organization key {string} and password {string}")
  public void deleteDatasetEndpoints(String datasetKey, String organisationKey, String password) throws Exception {
    result = mvc
      .perform(
        delete("/registry/service")
          .param("resourceKey", datasetKey)
          .contentType(APPLICATION_FORM_URLENCODED)
          .accept(APPLICATION_XML)
          .with(httpBasic(organisationKey, password)));
  }

  @When("delete endpoint without resource key using valid organization key {string} and password {string}")
  public void deleteDatasetEndpoints(String organisationKey, String password) throws Exception {
    result = mvc
      .perform(
        delete("/registry/service")
          .contentType(APPLICATION_FORM_URLENCODED)
          .accept(APPLICATION_XML)
          .with(httpBasic(organisationKey, password)));
  }

  @When("perform get all service types request")
  public void getAllTypes() throws Exception {
    result = mvc
      .perform(
        get("/registry/service{extension}", ".json")
          .param("op", "types")
          .contentType(TEXT_PLAIN));
  }

  @Then("response status should be {int}")
  public void assertResponseCode(int status) throws Exception {
    result
      .andExpect(status().is(status));
  }

  @Then("registered/updated endpoint is")
  public void checkEndpoint(LegacyEndpoint expectedEndpoint) throws Exception {
    String contentAsString = result.andReturn().getResponse().getContentAsString();
    actualEndpoint = xmlMapper.readValue(contentAsString, LegacyEndpoint.class);
    assertLenientEquals("Endpoints do not match", expectedEndpoint, actualEndpoint);
    assertNotNull(actualEndpoint.getKey());
    assertNotNull(actualEndpoint.getCreatedBy());
    assertNotNull(actualEndpoint.getModifiedBy());
  }

  @Then("response is following JSON")
  public void checkGetAllTypesResponse(List<Map<String, String>> expectedData) throws Exception {
    String contentAsString = result.andReturn().getResponse().getContentAsString();
    JsonNode actualData = objectMapper.readTree(contentAsString);

    for (int i = 0; i < expectedData.size(); i++) {
      assertEquals(expectedData.get(i).get("name"), actualData.get(i).get("name").asText());
      assertEquals(expectedData.get(i).get("overviewURL"), actualData.get(i).get("overviewURL").asText());
      assertEquals(expectedData.get(i).get("description"), actualData.get(i).get("description").asText());
      assertEquals(expectedData.get(i).get("key"), actualData.get(i).get("key").asText());
    }
  }
}
