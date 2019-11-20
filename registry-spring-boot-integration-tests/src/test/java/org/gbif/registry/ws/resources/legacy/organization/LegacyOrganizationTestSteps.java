package org.gbif.registry.ws.resources.legacy.organization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.ws.TestEmailConfiguration;
import org.gbif.registry.ws.model.ErrorResponse;
import org.gbif.registry.ws.model.LegacyOrganizationBriefResponse;
import org.gbif.registry.ws.model.LegacyOrganizationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {TestEmailConfiguration.class,
  RegistryIntegrationTestsConfiguration.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class LegacyOrganizationTestSteps {

  private MockMvc mvc;
  private ResultActions result;
  private LegacyOrganizationResponse actualResponse;
  private List<LegacyOrganizationBriefResponse> actualOrganizationsResponse;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private DataSource ds;

  private Connection connection;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private XmlMapper xmlMapper;

  @Before("@LegacyOrganization")
  public void setUp() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/legacyorganization/legacy_organization_cleanup.sql"));
    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/legacyorganization/legacy_organization_prepare.sql"));

    mvc = MockMvcBuilders
      .webAppContextSetup(context)
      .apply(springSecurity())
      .build();
  }

  @After("@LegacyOrganization")
  public void tearDown() throws Exception {
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/legacyorganization/legacy_organization_cleanup.sql"));

    connection.close();
  }

  @Given("node {string} with key {string}")
  public void prepareNode(String nodeName, String nodeKey) {
    // prepared by script legacy_organization_prepare
  }

  @Given("organization {string} with key {string}")
  public void prepareOrganization(String orgName, String orgKey) {
    // prepared by script legacy_organization_prepare
  }

  @Given("contact with key {string} of organization {string}")
  public void prepareOrganizationContact(String contactKey, String orgName) {
    // prepared by script legacy_organization_prepare
  }

  @When("get organization {string} with no credentials and extension {string}")
  public void getOrganization(String organisationKey, String extension) throws Exception {
    result = mvc
      .perform(
        get("/registry/organisation/{key}" + extension, organisationKey))
      .andDo(print());
  }

  @When("get organization {string} with login {string} and password {string} and extension {string} and parameter {word} with value {word}")
  public void getOrganization(String organisationKey, String login, String password, String extension, String paramName,
                              String paramValue) throws Exception {
    result = mvc
      .perform(
        get("/registry/organisation/{key}" + extension, organisationKey)
          .param(paramName, paramValue)
          .with(httpBasic(login, password)))
      .andDo(print());
  }

  @When("get organization {string} with extension {string} and parameter {word} with value {string}")
  public void getOrganization(String organisationKey, String extension, String paramName, String paramValue) throws Exception {
    result = mvc
      .perform(
        get("/registry/organisation/{key}" + extension, organisationKey)
          .param(paramName, paramValue))
      .andDo(print());
  }

  @When("get organizations with extension {string}")
  public void getOrganizations(String extension) throws Exception {
    result = mvc
      .perform(
        get("/registry/organisation{extension}", extension))
      .andDo(print());
  }

  @Then("response status should be {int}")
  public void assertResponseCode(int status) throws Exception {
    result
      .andExpect(status().is(status));
  }

  @Then("response should start with {string}")
  public void checkResponseStartsWith(String str) throws Exception {
    MvcResult mvcResult = result.andReturn();
    String contentAsString = mvcResult.getResponse().getContentAsString();
    assertTrue(contentAsString.startsWith(str));
  }

  @Then("returned response is {string}")
  public void checkErrorResponse(String errorResponseExpectedMessage) throws Exception {
    MvcResult mvcResult = result.andReturn();
    String contentAsString = mvcResult.getResponse().getContentAsString();
    ErrorResponse errorResponseActual = objectMapper.readValue(contentAsString, ErrorResponse.class);
    assertEquals(errorResponseExpectedMessage, errorResponseActual.getError());
  }

  @Then("{word} is expected")
  public void checkResponseProperFormatAndParse(String extension) throws Exception {
    MvcResult mvcResult = result.andReturn();
    String contentAsString = mvcResult.getResponse().getContentAsString();
    if ("JSON".equals(extension)) {
      actualResponse = objectMapper.readValue(contentAsString, LegacyOrganizationResponse.class);
    } else {
      actualResponse = xmlMapper.readValue(contentAsString, LegacyOrganizationResponse.class);
    }
  }

  @Then("valid {word} array is expected")
  public void checkArrayResponseProperFormatAndParse(String extension) throws Exception {
    MvcResult mvcResult = result.andReturn();
    String contentAsString = mvcResult.getResponse().getContentAsString();
    if ("JSON".equals(extension)) {
      actualOrganizationsResponse = objectMapper.readValue(contentAsString, new TypeReference<List<LegacyOrganizationBriefResponse>>() {
      });
    } else {
      // TODO: 20/11/2019 fix this, should start with lowercase 'l'
      assertTrue("Xml should start with <legacyOrganizationBriefResponses><organisation>",
        contentAsString.contains("<LegacyOrganizationBriefResponses><organisation>"));
      actualOrganizationsResponse = xmlMapper.readValue(contentAsString, new TypeReference<List<LegacyOrganizationBriefResponse>>() {
      });
    }
  }

  @Then("returned organization is")
  public void assertResponse(LegacyOrganizationResponse expectedResponse) {
    assertEquals(expectedResponse, actualResponse);
  }

  @Then("returned brief organizations information are")
  public void assertOrganizationsBriefResponse(List<LegacyOrganizationBriefResponse> expectedResponse) {
    assertEquals(expectedResponse, actualOrganizationsResponse);
  }
}
