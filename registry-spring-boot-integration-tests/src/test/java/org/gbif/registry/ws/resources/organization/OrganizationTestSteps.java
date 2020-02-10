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
package org.gbif.registry.ws.resources.organization;

import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.utils.Organizations;
import org.gbif.registry.utils.RegistryITUtils;
import org.gbif.registry.ws.resources.OrganizationResource;

import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.gbif.registry.ws.fixtures.TestConstants.TEST_ADMIN;
import static org.gbif.registry.ws.fixtures.TestConstants.TEST_PASSWORD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = {RegistryIntegrationTestsConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class OrganizationTestSteps {

  private static final UUID UK_NODE_KEY = UUID.fromString("f698c938-d36a-41ac-8120-c35903e1acb9");
  private static final UUID UK_NODE_2_KEY = UUID.fromString("9996f2f2-f71c-4f40-8e69-031917b314e0");

  private static final Map<String, UUID> NODE_MAP = new HashMap<>();

  private ResultActions result;
  private Organization organization;
  private String organizationKey;
  private Date modificationDateBeforeUpdate;
  private Date creationDateBeforeUpdate;

  private MockMvc mvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private WebApplicationContext context;

  @Autowired private DataSource ds;

  private Connection connection;

  @Autowired private OrganizationResource organizationResource;

  @Autowired private IdentityService identityService;

  @Before("@Organization")
  public void setUp() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/network_entities_cleanup.sql"));
    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/organization/organization_node_prepare.sql"));
    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/organization/organization_prepare.sql"));

    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    NODE_MAP.put("UK Node", UK_NODE_KEY);
    NODE_MAP.put("UK Node 2", UK_NODE_2_KEY);
  }

  @After("@Organization")
  public void tearDown() throws Exception {
    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/network_entities_cleanup.sql"));

    connection.close();
  }

  @Given("node {string} with key {string}")
  public void prepareNode(String nodeName, String nodeKey) {
    // see Before organization
  }

  @Given("seven organizations for node {string}")
  public void prepareOrganizations(String nodeName) {
    // see Before organization
  }

  @Given("user {string} with editor rights on node {string}")
  public void addEditorRights(String username, String nodeKey) {
    identityService.addEditorRight(username, UUID.fromString(nodeKey));
  }

  @When("^call suggest organizations with query \"([^\"]*)\"$")
  public void callSuggestWithQuery(String query) throws Exception {
    result = mvc.perform(get("/organization/suggest").param("q", query));
  }

  @Then("response status should be {int}")
  public void checkResponseStatus(int status) throws Exception {
    result.andExpect(status().is(status));
  }

  @Then("{int} organization\\(s) should be suggested")
  public void checkSuggestResponse(int number) throws Exception {
    result.andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$.length()").value(number));
  }

  @When("^call list organizations by country ([^\"]*)$")
  public void callListWithQuery(Country country) throws Exception {
    result =
        mvc.perform(get("/organization").param("country", country.getIso2LetterCode()))
            .andDo(print());
  }

  @Then("{int} organization\\(s) should be listed")
  public void checkListResponse(int number) throws Exception {
    result
        .andExpect(jsonPath("$.count").value(number))
        .andExpect(jsonPath("$.results.length()").value(number));
  }

  @When(
      "create new organization {string} for node {string} by {word} {string} and password {string}")
  public void createOrganization(
      String orgName, String nodeName, String userType, String username, String password)
      throws Exception {
    UUID nodeKey = NODE_MAP.get(nodeName);
    organization = Organizations.newInstance(nodeKey);
    organization.setTitle(orgName);
    String organizationJson = objectMapper.writeValueAsString(organization);

    result =
        mvc.perform(
            post("/organization")
                .with(httpBasic(username, password))
                .content(organizationJson)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));
  }

  @When("create new organization {string} for node {string}")
  public void createOrganization(String orgName, String nodeName) throws Exception {
    createOrganization(orgName, nodeName, "admin", TEST_ADMIN, TEST_PASSWORD);
  }

  @When("get organization by key")
  public void getOrganizationById() throws Exception {
    assertNotNull("Organization key should be present", organizationKey);
    result = mvc.perform(get("/organization/{key}", organizationKey)).andDo(print());
  }

  @Given("{int} organization\\(s) endorsed for {string}")
  public void checkNumberOfEndorsedOrganizationsForNode(int expected, String nodeName)
      throws Exception {
    UUID nodeKey = NODE_MAP.get(nodeName);
    mvc.perform(get("/node/{key}/organization", nodeKey))
        .andExpect(jsonPath("$.count").value(expected))
        .andExpect(jsonPath("$.results.length()").value(expected));
  }

  @Given("{int} organization\\(s) pending endorsement for {string}")
  public void checkNumberOfPendingEndorsementOrganizationForNode(int expected, String nodeName)
      throws Exception {
    UUID nodeKey = NODE_MAP.get(nodeName);
    mvc.perform(get("/node/{key}/pendingEndorsement", nodeKey))
        .andExpect(jsonPath("$.count").value(expected))
        .andExpect(jsonPath("$.results.length()").value(expected));
  }

  @Given("{int} organization\\(s) pending endorsement in total")
  public void checkNumberOfPendingEndorsementOrganizationTotal(int expected) throws Exception {
    mvc.perform(get("/node/pendingEndorsement"))
        .andExpect(jsonPath("$.count").value(expected))
        .andExpect(jsonPath("$.results.length()").value(expected));
    ;
  }

  @When("endorse organization {string}")
  public void endorseOrganization(String orgName) throws Exception {
    organization = organizationResource.get(UUID.fromString(organizationKey));
    assertNotNull(organization);
    organization.setEndorsementApproved(true);

    String organizationJson = objectMapper.writeValueAsString(organization);

    mvc.perform(
        put("/organization/{key}", organizationKey)
            .with(httpBasic(TEST_ADMIN, TEST_PASSWORD))
            .content(organizationJson)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON));
  }

  @When("create new organization for {string} with key")
  public void createOrganizationWithKey(String nodeName) throws Exception {
    UUID nodeKey = NODE_MAP.get(nodeName);
    organization = Organizations.newInstance(nodeKey);
    organization.setKey(UUID.randomUUID());

    String organizationJson = objectMapper.writeValueAsString(organization);

    result =
        mvc.perform(
            post("/organization")
                .with(httpBasic(TEST_ADMIN, TEST_PASSWORD))
                .content(organizationJson)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));
  }

  @When("get titles for empty list")
  public void getTitlesForEmptyList() throws Exception {
    Collection<UUID> emptyList = Collections.emptyList();

    String contentJson = objectMapper.writeValueAsString(emptyList);

    result =
        mvc.perform(
            post("/organization/titles")
                .content(contentJson)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));
  }

  @Then("empty titles map is returned")
  public void checkEmptyTitlesMap() throws Exception {
    MvcResult mvcResult = result.andReturn();

    String contentAsString = mvcResult.getResponse().getContentAsString();
    Map responseBody = objectMapper.readValue(contentAsString, Map.class);

    assertEquals(Collections.emptyMap(), responseBody);
  }

  @When("get titles for organizations")
  public void getTitlesForOrganizations(List<String> keys) throws Exception {
    String contentJson = objectMapper.writeValueAsString(keys);

    result =
        mvc.perform(
            post("/organization/titles")
                .content(contentJson)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));
  }

  @Then("titles map is returned")
  public void checkTitlesMap(Map expected) throws Exception {
    MvcResult mvcResult = result.andReturn();

    String contentAsString = mvcResult.getResponse().getContentAsString();
    Map responseBody = objectMapper.readValue(contentAsString, Map.class);

    assertEquals(expected, responseBody);
  }

  @When("update organization {string} with new title {string}")
  public void updateOrganization(String orgName, String newTitle) throws Exception {
    organization = organizationResource.get(UUID.fromString(organizationKey));
    assertNotNull(organization);
    modificationDateBeforeUpdate = organization.getModified();
    creationDateBeforeUpdate = organization.getCreated();
    organization.setTitle(newTitle);

    String organizationJson = objectMapper.writeValueAsString(organization);

    result =
        mvc.perform(
                put("/organization/{key}", organizationKey)
                    .with(httpBasic(TEST_ADMIN, TEST_PASSWORD))
                    .content(organizationJson)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(print());
  }

  @When("delete organization {string} by key")
  public void deleteOrganization(String orgName) throws Exception {
    result =
        mvc.perform(
            delete("/organization/{key}", organizationKey)
                .with(httpBasic(TEST_ADMIN, TEST_PASSWORD)));
  }

  @Then("title is new {string}")
  public void checkUpdatedOrganizationTitle(String newTitle) {
    assertEquals("Organization's title was to be updated", newTitle, organization.getTitle());
  }

  @Then("modification date was updated")
  public void checkModificationDateWasChangedAfterUpdate() {
    assertNotNull(organization.getModified());
    assertTrue(
        "Modification date was to be changed",
        organization.getModified().after(modificationDateBeforeUpdate));
  }

  @Then("modification date is after the creation date")
  public void checkModificationDateIsAfterCreationDate() {
    assertNotNull(organization.getModified());
    assertTrue(
        "Modification date must be after the creation date",
        organization.getModified().after(creationDateBeforeUpdate));
  }

  @Then("update organization with new invalid too short title {string} for node {string}")
  public void testUpdateValidationFailing(String orgTitle, String nodeName) throws Exception {
    organization = organizationResource.get(UUID.fromString(organizationKey));
    assertNotNull(organization);
    organization.setTitle(orgTitle); // should fail as it is too short

    String organizationJson = objectMapper.writeValueAsString(organization);

    result =
        mvc.perform(
            put("/organization/{key}", organizationKey)
                .with(httpBasic(TEST_ADMIN, TEST_PASSWORD))
                .content(organizationJson)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));
  }

  @Then("organization key is present in response")
  public void extractKeyFromResponse() throws Exception {
    organizationKey =
        RegistryITUtils.removeQuotes(result.andReturn().getResponse().getContentAsString());
  }

  @Then("organization is")
  public void checkOrganization(Map<String, String> expectedOrganization) throws Exception {
    result.andExpect(jsonPath("$.").value(""));
  }
}
