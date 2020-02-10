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
package org.gbif.registry.ws.resources.dataset;

import org.gbif.api.model.registry.Dataset;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.utils.Datasets;
import org.gbif.registry.utils.RegistryITUtils;
import org.gbif.registry.ws.resources.DatasetResource;

import java.sql.Connection;
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

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.gbif.registry.ws.fixtures.TestConstants.TEST_ADMIN;
import static org.gbif.registry.ws.fixtures.TestConstants.TEST_PASSWORD;
import static org.junit.Assert.assertNotNull;
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
public class DatasetTestSteps {

  private MockMvc mvc;
  private ResultActions result;
  private UUID datasetKey;
  private Dataset dataset;
  private UUID orgKey = UUID.fromString("36107c15-771c-4810-a298-b7558828b8bd");
  private UUID installationKey = UUID.fromString("2fe63cec-9b23-4974-bab1-9f4118ef7711");

  @Autowired private ObjectMapper objectMapper;

  @Autowired private DatasetResource datasetResource;

  @Autowired private WebApplicationContext context;

  @Autowired private DataSource ds;

  private Connection connection;

  @Autowired private IdentityService identityService;

  @Before("@Dataset")
  public void setUp() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/network_entities_cleanup.sql"));
    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/dataset/dataset_prepare.sql"));

    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @After("@Dataset")
  public void tearDown() throws Exception {
    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/network_entities_cleanup.sql"));

    connection.close();
  }

  @Given("user {string} with editor rights on organization {string}")
  public void addEditorRights(String username, String orgKey) {
    identityService.addEditorRight(username, UUID.fromString(orgKey));
  }

  @When("create new dataset {string} for installation {string} and organization {string}")
  public void createDataset(String datasetName, String installationName, String orgName)
      throws Exception {
    createDataset(datasetName, installationName, orgName, "admin", TEST_ADMIN, TEST_PASSWORD);
  }

  @When(
      "create new dataset {string} for installation {string} and organization {string} by {word} {string} and password {string}")
  public void createDataset(
      String datasetName,
      String installationName,
      String orgName,
      String userType,
      String username,
      String password)
      throws Exception {
    Dataset dataset = Datasets.newInstance(orgKey, installationKey);
    dataset.setTitle(datasetName);

    String jsonContent = objectMapper.writeValueAsString(dataset);

    result =
        mvc.perform(
            post("/dataset")
                .with(httpBasic(username, password))
                .content(jsonContent)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));
  }

  @When("update dataset {string}")
  public void updateDataset(String datasetName) throws Exception {
    dataset = datasetResource.get(datasetKey);
    assertNotNull(dataset);

    String jsonContent = objectMapper.writeValueAsString(dataset);

    result =
        mvc.perform(
                put("/dataset/{key}", datasetKey)
                    .with(httpBasic(TEST_ADMIN, TEST_PASSWORD))
                    .content(jsonContent)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON))
            .andDo(print());
  }

  @When("delete dataset {string} by key")
  public void deleteDataset(String datasetName) throws Exception {
    result =
        mvc.perform(
            delete("/dataset/{key}", datasetKey).with(httpBasic(TEST_ADMIN, TEST_PASSWORD)));
  }

  @When("get dataset by key")
  public void getDatasetById() throws Exception {
    result = mvc.perform(get("/dataset/{key}", datasetKey));
  }

  @Then("response status should be {int}")
  public void assertResponseCode(int status) throws Exception {
    result.andExpect(status().is(status));
  }

  @Then("dataset key is present in response")
  public void extractKeyFromResponse() throws Exception {
    datasetKey =
        UUID.fromString(
            RegistryITUtils.removeQuotes(result.andReturn().getResponse().getContentAsString()));
  }
}
