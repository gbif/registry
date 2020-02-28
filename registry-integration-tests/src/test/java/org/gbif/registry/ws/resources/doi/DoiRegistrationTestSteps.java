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
package org.gbif.registry.ws.resources.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.AlternateIdentifiers;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.AlternateIdentifiers.AlternateIdentifier;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators.Creator;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators.Creator.CreatorName;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Publisher;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Titles;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Titles.Title;
import org.gbif.doi.metadata.datacite.ResourceType;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.doi.registration.DoiRegistration;
import org.gbif.registry.domain.doi.DoiType;
import org.gbif.registry.utils.RegistryITUtils;

import java.sql.Connection;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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
import static org.junit.Assert.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    classes = {RegistryIntegrationTestsConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class DoiRegistrationTestSteps {

  private ResultActions result;
  private MockMvc mvc;
  private DOI doi;

  @Autowired private WebApplicationContext context;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private DataSource ds;

  private Connection connection;

  @Before("@DoiRegistration")
  public void setUp() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(connection, new ClassPathResource("/scripts/doi/doi_cleanup.sql"));
    ScriptUtils.executeSqlScript(connection, new ClassPathResource("/scripts/doi/doi_prepare.sql"));

    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @After("@DoiRegistration")
  public void cleanAfterUserCrud() throws Exception {
    Objects.requireNonNull(connection, "Connection must not be null");
    ScriptUtils.executeSqlScript(connection, new ClassPathResource("/scripts/doi/doi_cleanup.sql"));

    connection.close();
  }

  @Given("existing DOI {string} with status {string}")
  public void prepareDoi(String doi, String doiStatus) {
    // prepared by scripts in @Before
  }

  @When("generate new DOI of type {string} by {word}")
  public void changePassword(String doiType, String userType) throws Exception {
    MockHttpServletRequestBuilder requestBuilder =
        post("/doi/gen/{type}", doiType).contentType(MediaType.APPLICATION_JSON);

    if (userType.equals("admin")) {
      requestBuilder.with(httpBasic(TEST_ADMIN, TEST_PASSWORD));
    }

    result = mvc.perform(requestBuilder);
  }

  @When("register DOI of type {string} for entity with key {string} and metadata parameters")
  public void registerDoi(String doiType, String key, Map<String, String> params) throws Exception {
    registerDoi(null, doiType, key, "admin", params);
  }

  @When(
      "register DOI {string} of type {string} for entity with key {string} and metadata parameters by {word}")
  public void registerDoi(
      String doiStr, String doiType, String key, String userType, Map<String, String> params)
      throws Exception {
    DoiRegistration data =
        DoiRegistration.builder()
            .withType(DoiType.valueOf(doiType))
            .withUser(TEST_ADMIN)
            .withMetadata(testMetadata(params, doiStr))
            .build();

    if (doiStr != null) {
      data.setDoi(new DOI(doiStr));
    }

    if (!"DATA_PACKAGE".equals(doiType)) {
      data.setKey(key);
    }

    String jsonContent = objectMapper.writeValueAsString(data);

    MockHttpServletRequestBuilder requestBuilder =
        post("/doi", doiType).content(jsonContent).contentType(MediaType.APPLICATION_JSON);

    if (userType.equals("admin")) {
      requestBuilder.with(httpBasic(TEST_ADMIN, TEST_PASSWORD));
    }

    result = mvc.perform(requestBuilder);
  }

  @When("update DOI of type {string} for entity with key {string} and metadata parameters")
  public void updateDoi(String doiType, String key, Map<String, String> params) throws Exception {
    updateDoi(null, doiType, key, "admin", params);
  }

  @When(
      "update DOI {string} of type {string} for entity with key {string} and metadata parameters by {word}")
  public void updateDoi(
      String doiStr, String doiType, String key, String userType, Map<String, String> params)
      throws Exception {
    DoiRegistration data =
        DoiRegistration.builder()
            .withType(DoiType.valueOf(doiType))
            .withUser(TEST_ADMIN)
            .withMetadata(testMetadata(params, doiStr))
            .build();

    if (doiStr != null) {
      data.setDoi(new DOI(doiStr));
    }

    if (!"DATA_PACKAGE".equals(doiType)) {
      data.setKey(key);
    }

    String jsonContent = objectMapper.writeValueAsString(data);

    MockHttpServletRequestBuilder requestBuilder =
        put("/doi", doiType).content(jsonContent).contentType(MediaType.APPLICATION_JSON);

    if (userType.equals("admin")) {
      requestBuilder.with(httpBasic(TEST_ADMIN, TEST_PASSWORD));
    }

    result = mvc.perform(requestBuilder);
  }

  @When("delete DOI of type {string}")
  public void deleteDoi(String doiType) throws Exception {
    result = mvc.perform(delete("/doi/{prefix}/{suffix}", doi.getPrefix(), doi.getSuffix()));
  }

  @When("get DOI")
  public void getDoi() throws Exception {
    result =
        mvc.perform(
                get("/doi/{prefix}/{suffix}", doi.getPrefix(), doi.getSuffix())
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(print());
  }

  @When("get DOI {string}")
  public void getDoi(String doiStr) throws Exception {
    DOI doi = doiStr != null ? new DOI(doiStr) : this.doi;
    result =
        mvc.perform(
                get("/doi/{prefix}/{suffix}", doi.getPrefix(), doi.getSuffix())
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(print());
  }

  @Then("response status should be {int}")
  public void assertResponseCode(int status) throws Exception {
    result.andExpect(status().is(status));
  }

  @Then("DOI is returned")
  public void assertDoiWasReturned() throws Exception {
    String doiStr =
        RegistryITUtils.removeQuotes(result.andReturn().getResponse().getContentAsString());
    doi = new DOI(doiStr);
    assertFalse(doiStr.isEmpty());
  }

  /** Create a test DataCiteMetadata instance. */
  private String testMetadata(Map<String, String> params, String doiStr) {
    try {
      DataCiteMetadata.Builder<Void> builder = DataCiteMetadata.builder();

      builder.withCreators(
          Creators.builder()
              .addCreator(
                  Creator.builder()
                      .withCreatorName(
                          CreatorName.builder()
                              .withValue(params.getOrDefault("creator", TEST_ADMIN))
                              .build())
                      .build())
              .build());

      builder.withTitles(
          Titles.builder()
              .addTitle(
                  Title.builder().withValue(params.getOrDefault("title", "TEST Title")).build())
              .build());

      builder.withPublicationYear(params.getOrDefault("publicationYear", "2019"));

      builder.withPublisher(
          Publisher.builder().withValue(params.getOrDefault("publisher", TEST_ADMIN)).build());

      builder.withResourceType(
          DataCiteMetadata.ResourceType.builder()
              .withResourceTypeGeneral(ResourceType.DATASET)
              .build());

      builder.withAlternateIdentifiers(
          AlternateIdentifiers.builder()
              .addAlternateIdentifier(
                  AlternateIdentifier.builder()
                      .withValue(
                          params.getOrDefault("alternateIdentifier", "0000364-150202100118378"))
                      .withAlternateIdentifierType(
                          params.getOrDefault("alternateIdentifierType", "GBIF"))
                      .build())
              .build());

      DataCiteMetadata metadata = builder.build();

      return DataCiteValidator.toXml(
          doiStr != null ? new DOI(doiStr) : new DOI(DOI.TEST_PREFIX, "1"), metadata);
    } catch (InvalidMetadataException ex) {
      throw new RuntimeException(ex);
    }
  }
}
