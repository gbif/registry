package org.gbif.registry.ws.resources.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.utils.Organizations;
import org.gbif.registry.utils.RegistryITUtils;
import org.gbif.registry.ws.TestEmailConfiguration;
import org.gbif.registry.ws.resources.SpringIT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {TestEmailConfiguration.class, RegistryIntegrationTestsConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class OrganizationTestSteps extends SpringIT {

  private static final UUID NODE_KEY = UUID.fromString("f698c938-d36a-41ac-8120-c35903e1acb9");

  private ResultActions result;

  private Organization organization;

  private MockMvc mvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private DataSource ds;

  @Before("@OrganizationPositive")
  public void setUp() throws Exception {
    ScriptUtils.executeSqlScript(ds.getConnection(),
        new ClassPathResource("/scripts/organization/organization_cleanup.sql"));

    mvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  @After("@OrganizationPositive")
  public void tearDown() throws Exception {
    ScriptUtils.executeSqlScript(ds.getConnection(),
        new ClassPathResource("/scripts/organization/organization_cleanup.sql"));
  }

  @Given("^node$")
  public void prepareNode() throws Exception {
    ScriptUtils.executeSqlScript(ds.getConnection(),
        new ClassPathResource("/scripts/organization/organization_node_prepare.sql"));
  }

  @Given("^seven organizations$")
  public void prepareOrganizations() throws Exception {
    ScriptUtils.executeSqlScript(ds.getConnection(),
        new ClassPathResource("/scripts/organization/organization_prepare.sql"));
  }

  @When("^call suggest organizations with query \"([^\"]*)\"$")
  public void callSuggestWithQuery(String query) throws Exception {
    result = mvc
        .perform(
            get("/organization/suggest")
                .param("q", query));
  }

  @Then("^response status should be \"([^\"]*)\"$")
  public void checkResponseStatus(int status) throws Exception {
    result
        .andExpect(status().is(status));
  }

  @Then("^\"([^\"]*)\" organization\\(s\\) should be suggested$")
  public void checkSuggestResponse(int number) throws Exception {
    result
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(number));
  }

  @When("^call list organizations by country \"([^\"]*)\"$")
  public void callListWithQuery(Country country) throws Exception {
    result = mvc
        .perform(
            get("/organization")
                .param("country", country.getIso2LetterCode()));
  }

  @Then("^\"([^\"]*)\" organization\\(s\\) should be listed$")
  public void checkListResponse(int expectedNumber) throws Exception {
    result
        .andExpect(jsonPath("$.count").value(expectedNumber))
        .andExpect(jsonPath("$.results.length()").value(expectedNumber));
  }

  @Given("^new not created organization$")
  public void prepareOrganization() throws Exception {
    organization = Organizations.newInstance(NODE_KEY);
  }

  @When("^try to create that organization$")
  public void createOrganization() throws Exception {
    String organizationJson = objectMapper.writeValueAsString(organization);

    result = mvc
        .perform(
            post("/organization")
                .with(httpBasic("justadmin", "welcome"))
                .content(organizationJson)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));
  }

  @When("^get organization by id$")
  public void getOrganizationById() throws Exception {
    // get an id for the created organization (remove quotes from string)
    String createdOrganizationKey =
        RegistryITUtils.removeQuotes(result.andReturn().getResponse().getContentAsString());

    // try to get organization by key
    mvc
        .perform(
            get("/organization/{key}", createdOrganizationKey));

    // TODO: 16/10/2019 assertLenientEquals and stuff (see registry's NetworkEntityTest)
  }

}
