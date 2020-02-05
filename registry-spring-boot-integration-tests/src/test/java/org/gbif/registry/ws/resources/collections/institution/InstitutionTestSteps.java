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
package org.gbif.registry.ws.resources.collections.institution;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.utils.RegistryITUtils;

import java.net.URI;
import java.sql.Connection;
import java.util.Collections;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.gbif.registry.ws.fixtures.TestConstants.TEST_PASSWORD;
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
public class InstitutionTestSteps {

  private ResultActions result;
  private MockMvc mvc;

  @Autowired private ObjectMapper objectMapper;
  @Autowired private WebApplicationContext context;
  @Autowired private DataSource ds;
  private Connection connection;
  @Autowired private InstitutionService institutionService;

  private Institution institution;
  private UUID institutionKey;

  @Before("@Institution")
  public void setUp() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/institution/institution_cleanup.sql"));
    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/institution/institution_prepare.sql"));

    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @After("@Institution")
  public void tearDown() throws Exception {
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/institution/institution_cleanup.sql"));

    connection.close();
  }

  @Given("{int} addresses")
  public void givenAddresses(int number, DataTable dataTable) {
    // See Before Institution
  }

  @Given("{int} contacts")
  public void givenContacts(int number, DataTable dataTable) {
    // See Before Institution
  }

  @Given("{int} institutions")
  public void givenInstitutions(int number, DataTable dataTable) {
    // See Before Institution
  }

  @Given("new institution")
  public void givenInstitution(Institution institution) {
    this.institution = institution;
  }

  @Given("new institution address")
  public void givenInstitutionAddress(Address address) {
    institution.setAddress(address);
  }

  @Given("new institution mailing address")
  public void givenInstitutionMailingAddress(Address mailingAddress) {
    institution.setMailingAddress(mailingAddress);
  }

  @Given("new institution tags")
  public void givenInstitutionTags(List<Tag> tags) {
    institution.setTags(tags);
  }

  @Given("new institution identifiers")
  public void givenInstitutionIdentifiers(List<Identifier> identifiers) {
    institution.setIdentifiers(identifiers);
  }

  @Given("new arbitrary valid institution {string}")
  public void prepareArbitraryInstitution(String institutionName) {
    institution = new Institution();
    institution.setCode("ABC");
    institution.setName(institutionName);
    institution.setDescription("fake institution for IT");
    institution.setHomepage(URI.create("https://www.gbif.org/"));
    institution.setAdditionalNames(Collections.emptyList());
  }

  @Given("institution {string} with key {string}")
  public void prepareInstitutionKey(String institutionName, String institutionKey) {
    this.institutionKey = UUID.fromString(institutionKey);
    this.institution = institutionService.get(this.institutionKey);
  }

  @When("call suggest institutions with query {string}")
  public void suggestInstitutions(String query) throws Exception {
    result = mvc.perform(get("/grscicoll/institution/suggest").param("q", query)).andDo(print());
  }

  @When("list deleted institutions")
  public void listDeletedInstitutions() throws Exception {
    result = mvc.perform(get("/grscicoll/institution/deleted")).andDo(print());
  }

  @When("list institutions by query {string}")
  public void listInstitutionsByQuery(String query) throws Exception {
    result = mvc.perform(get("/grscicoll/institution").param("q", query)).andDo(print());
  }

  @When("list institutions by contact {string}")
  public void listInstitutionsByContact(String contact) throws Exception {
    result = mvc.perform(get("/grscicoll/institution").param("contact", contact)).andDo(print());
  }

  @When("delete institution {string} using {word} {string}")
  public void deleteInstitution(String institutionName, String userType, String username)
      throws Exception {
    result =
        mvc.perform(
                delete("/grscicoll/institution/{key}", institutionKey)
                    .with(httpBasic(username, TEST_PASSWORD)))
            .andDo(print());
  }

  @When("create institution {string} using {word} {string}")
  public void createInstitution(String institutionName, String userType, String username)
      throws Exception {
    String content = objectMapper.writeValueAsString(institution);

    result =
        mvc.perform(
                post("/grscicoll/institution")
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(httpBasic(username, TEST_PASSWORD)))
            .andDo(print());
  }

  @When("get institution by key")
  public void getInstitution() throws Exception {
    institution = institutionService.get(institutionKey);
    result = mvc.perform(get("/grscicoll/institution/{key}", institutionKey)).andDo(print());
  }

  @When("update institution {string} using {word} {string}")
  public void updateInstitution(
      String institutionName, String userType, String username, Map<String, String> params)
      throws Exception {
    String content = objectMapper.writeValueAsString(institution);

    result =
        mvc.perform(
                put("/grscicoll/institution/{key}", institutionKey)
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(httpBasic(username, TEST_PASSWORD)))
            .andDo(print());
  }

  @Then("response status should be {int}")
  public void assertResponseCode(int status) throws Exception {
    result.andExpect(status().is(status));
  }

  @Then("{int} institution\\(s) should be suggested")
  public void assertInstitutionsSuggested(int suggestedNumber) throws Exception {
    result
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(suggestedNumber));
  }

  @Then("{int} institution\\(s) in response")
  public void assertInstitutionListResponse(int expected) throws Exception {
    result
        .andExpect(jsonPath("$.endOfRecords").value(true))
        .andExpect(jsonPath("$.results.length()").value(expected));
  }

  @Then("institution key is present in response")
  public void extractKeyFromResponse() throws Exception {
    institutionKey =
        UUID.fromString(
            RegistryITUtils.removeQuotes(result.andReturn().getResponse().getContentAsString()));
  }
}
