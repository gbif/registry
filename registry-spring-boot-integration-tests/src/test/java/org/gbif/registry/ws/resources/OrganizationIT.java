package org.gbif.registry.ws.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gbif.api.model.registry.Organization;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.utils.Organizations;
import org.gbif.registry.utils.RegistryITUtils;
import org.gbif.registry.ws.TestEmailConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// TODO: 14/10/2019 add NetworkEntityTest
@Sql(value = {"/scripts/organization/organization_cleanup.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(value = {"/scripts/organization/organization_prepare.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(value = "/scripts/organization/organization_cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@SpringBootTest(classes = {TestEmailConfiguration.class, RegistryIntegrationTestsConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class OrganizationIT {

  private static final UUID NODE_KEY = UUID.fromString("f698c938-d36a-41ac-8120-c35903e1acb9");

  @Autowired
  private ObjectMapper objectMapper;

  private MockMvc mvc;

  @Autowired
  private WebApplicationContext context;

  @Before
  public void setUp() {
    mvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  /**
   * There are several prepared organization in DB (see scripts/organization).
   * By the query 'BGBM' six should be suggested.
   */
  @Test
  public void testSuggestAllExistedOrganizationsShouldBeReturned() throws Exception {
    String query = "BGBM";
    mvc
        .perform(
            get("/organization/suggest")
                .param("q", query))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(6));
  }

  /**
   * There are several prepared organization in DB (see scripts/organization).
   * By the query 'ORG' only one organization should be suggested.
   */
  @Test
  public void testSuggestOnlyOneExistedOrganizationShouldBeReturned() throws Exception {
    // Should find only The ORG
    String query = "ORG";
    mvc
        .perform(
            get("/organization/suggest")
                .param("q", query))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(1));
  }

  /**
   * There are several prepared organization in DB (see scripts/organization).
   * By the query 'Stuff' zero organizations should be suggested.
   */
  @Test
  public void testSuggestZeroOrganizationShouldBeReturned() throws Exception {
    // Should find zero
    String query = "Stuff";
    mvc
        .perform(
            get("/organization/suggest")
                .param("q", query))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  public void testByCountryTwoCountriesWithProvidedCode() throws Exception {
    String countryQuery = "AO";
    mvc
        .perform(
            get("/organization")
                .param("country", countryQuery))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").value(2))
        .andExpect(jsonPath("$.results.length()").value(2));
  }

  @Test
  public void testByCountryZeroCountriesWithProvidedCode() throws Exception {
    String countryQuery = "AM";
    mvc
        .perform(
            get("/organization")
                .param("country", countryQuery))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").value(0))
        .andExpect(jsonPath("$.results.length()").value(0));
  }

  @Test
  public void testCreate() throws Exception {
    Organization organization = Organizations.newInstance(NODE_KEY);
    String organizationJson = objectMapper.writeValueAsString(organization);

    // create a new organization with admin rights
    MvcResult mvcResult = mvc
        .perform(
            post("/organization")
                .with(httpBasic("justadmin", "welcome"))
                .content(organizationJson)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isCreated())
        .andReturn();

    // get an id for the created organization (remove quotes from string)
    String createdOrganizationKey =
        RegistryITUtils.removeQuotes(mvcResult.getResponse().getContentAsString());

    // try to get organization by key
    mvc
        .perform(
            get("/organization/{key}", createdOrganizationKey))
        .andDo(print())
        .andExpect(status().isOk());

    // TODO: 16/10/2019 assertLenientEquals and stuff (see registry's NetworkEntityTest)
  }
}
