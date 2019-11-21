package org.gbif.registry.ws.resources.legacy.endpoint;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.ws.TestEmailConfiguration;
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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.gbif.registry.ws.util.LegacyResourceConstants.ACCESS_POINT_URL_PARAM;
import static org.gbif.registry.ws.util.LegacyResourceConstants.DESCRIPTION_PARAM;
import static org.gbif.registry.ws.util.LegacyResourceConstants.RESOURCE_KEY_PARAM;
import static org.gbif.registry.ws.util.LegacyResourceConstants.TYPE_PARAM;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {TestEmailConfiguration.class,
  RegistryIntegrationTestsConfiguration.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class LegacyEndpointTestSteps {

  private MockMvc mvc;
  private ResultActions result;
  private HttpHeaders requestParamsEndpoint;
  private Installation actualInstallation;
  private Dataset actualDataset;
  private UUID installationKey;
  private UUID datasetKey;

  @Autowired
  private DatasetService datasetService;

  @Autowired
  private InstallationService installationService;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private DataSource ds;

  private Connection connection;

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
    this.installationKey = UUID.fromString(installationKey);
    actualInstallation = installationService.get(this.installationKey);
  }

  @Given("dataset {string} with key {string}")
  public void prepareDataset(String name, String datasetKey) {
    this.datasetKey = UUID.fromString(datasetKey);
    actualDataset = datasetService.get(this.datasetKey);
  }

  @Given("query parameters for endpoint registration or updating")
  public void prepareRequestParamsEndpoint(Map<String, String> params) {
    requestParamsEndpoint = new HttpHeaders();
    requestParamsEndpoint.add(RESOURCE_KEY_PARAM, params.get(RESOURCE_KEY_PARAM));
    requestParamsEndpoint.add(DESCRIPTION_PARAM, params.get(DESCRIPTION_PARAM));
    requestParamsEndpoint.add(TYPE_PARAM, params.get(TYPE_PARAM));
    requestParamsEndpoint.add(ACCESS_POINT_URL_PARAM, params.get(ACCESS_POINT_URL_PARAM));
  }

  @When("register new endpoint using valid/invalid organization key {string} and password {string}")
  public void registerEndpoint(String organisationKey, String password) throws Exception {
    result = mvc
      .perform(
        post("/registry/service")
          .params(requestParamsEndpoint)
          .contentType(APPLICATION_FORM_URLENCODED)
          .accept(APPLICATION_XML)
          .with(httpBasic(organisationKey, password)))
      .andDo(print());
  }

  @Then("response status should be {int}")
  public void assertResponseCode(int status) throws Exception {
    result
      .andExpect(status().is(status));
  }
}
