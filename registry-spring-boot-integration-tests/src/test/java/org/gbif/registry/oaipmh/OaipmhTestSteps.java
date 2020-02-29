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
package org.gbif.registry.oaipmh;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static java.time.ZoneOffset.UTC;
import static org.junit.Assert.assertNotNull;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@SpringBootTest(
    classes = {RegistryIntegrationTestsConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class OaipmhTestSteps {

  private ResultActions result;
  private MockMvc mvc;
  private DataSource ds;
  private Connection connection;
  private Map<String, List<String>> requestParams;
  private DatasetService datasetService;

  public OaipmhTestSteps(
      WebApplicationContext webContext, DataSource ds, DatasetService datasetService) {
    mvc = MockMvcBuilders.webAppContextSetup(webContext).apply(springSecurity()).build();
    this.ds = ds;
    this.datasetService = datasetService;
  }

  @Before("@OaipmhGetRecord")
  public void before() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/oaipmh/oaipmh_get_record_prepare.sql"));
  }

  @After("@OaipmhGetRecord")
  public void after() throws Exception {
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/oaipmh/oaipmh_get_record_cleanup.sql"));

    connection.close();
  }

  @Given("node")
  public void prepareNode(DataTable dataTable) {
    // prepared by scripts
  }

  @Given("organization")
  public void prepareOrganization(DataTable dataTable) {
    // prepared by scripts
  }

  @Given("installation")
  public void prepareInstallation(DataTable dataTable) {
    // prepared by scripts
  }

  @Given("dataset")
  public void prepareDataset(DataTable dataTable) {
    // prepared by scripts
  }

  @Given("metadata")
  public void prepareMetadata(DataTable dataTable) {
    // prepared by scripts
  }

  @When("Get record by parameters")
  public void getRecord(Map<String, List<String>> parameters) throws Exception {
    this.requestParams = parameters;
    result =
        mvc.perform(get("/oai-pmh/registry").params(new LinkedMultiValueMap<>(requestParams)))
            .andDo(print());
  }

  @Then("response status is {int}")
  public void checkResponseStatus(int status) throws Exception {
    result.andExpect(status().is(status));
  }

  @Then("request parameters in response are correct")
  public void checkRequestParams() throws Exception {
    result
        .andExpect(xpath("string(/OAI-PMH/request/@verb)").string(requestParams.get("verb").get(0)))
        .andExpect(
            xpath("string(/OAI-PMH/request/@metadataPrefix)")
                .string(requestParams.get("metadataPrefix").get(0)));
  }

  @Then("error code is {string}")
  public void checkError(String error) throws Exception {
    result.andExpect(xpath("string(/OAI-PMH/error/@code)").string(error));
  }

  @Then("no error in response")
  public void checkNoError() throws Exception {
    result.andExpect(xpath("/OAI-PMH/error").doesNotExist());
  }

  @Then("response contains processed citation {string}")
  public void someResultIsExpected(String rawCitation) throws Exception {
    String citation = String.format(rawCitation, LocalDate.now(UTC));
    result.andExpect(
        xpath("/OAI-PMH/GetRecord/record/metadata/eml/additionalMetadata/metadata/gbif/citation")
            .string(citation));
  }

  @When("restore dataset {string}")
  public void restoreDataset(String datasetKey) {
    Dataset dataset = datasetService.get(UUID.fromString(datasetKey));
    assertNotNull(dataset.getDeleted());

    dataset.setDeleted(null);
    datasetService.update(dataset);
  }

  @When("delete dataset {string}")
  public void deleteDataset(String datasetKey) {
    datasetService.delete(UUID.fromString(datasetKey));
  }

  @And("no record status")
  public void datasetIsNotDeleted() throws Exception {
    result.andExpect(xpath("/OAI-PMH/GetRecord/record/header/@status").doesNotExist());
  }

  @And("record status is {string}")
  public void checkRecordStatus(String recordStatus) throws Exception {
    result.andExpect(
        xpath("string(/OAI-PMH/GetRecord/record/header/@status)").string(recordStatus));
  }
}
