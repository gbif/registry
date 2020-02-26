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

import org.gbif.registry.RegistryIntegrationTestsConfiguration;

import java.sql.Connection;

import javax.sql.DataSource;

import org.dspace.xoai.model.oaipmh.MetadataFormat;
import org.dspace.xoai.model.oaipmh.Record;
import org.dspace.xoai.serviceprovider.ServiceProvider;
import org.dspace.xoai.serviceprovider.client.HttpOAIClient;
import org.dspace.xoai.serviceprovider.client.OAIClient;
import org.dspace.xoai.serviceprovider.exceptions.CannotDisseminateFormatException;
import org.dspace.xoai.serviceprovider.exceptions.IdDoesNotExistException;
import org.dspace.xoai.serviceprovider.model.Context;
import org.dspace.xoai.serviceprovider.parameters.GetRecordParameters;
import org.junit.Ignore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTest(
    classes = {RegistryIntegrationTestsConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Ignore
public class OaipmhTestSteps {

  private MockMvc mvc;
  private ResultActions result;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private WebApplicationContext context;

  @Autowired private DataSource ds;

  private Connection connection;

  private final String baseUrl;
  private final ServiceProvider serviceProvider;
  private GetRecordParameters recordParameters;
  private Exception actualException;
  private Record record;

  private String BASE_URL_FORMAT = "http://localhost:%d/oai-pmh/registry";

  protected MetadataFormat OAIDC_FORMAT =
      new MetadataFormat()
          .withMetadataPrefix("oai_dc")
          .withMetadataNamespace("http://www.openarchives.org/OAI/2.0/oai_dc/")
          .withSchema("http://www.openarchives.org/OAI/2.0/oai_dc.xsd");

  protected MetadataFormat EML_FORMAT =
      new MetadataFormat()
          .withMetadataPrefix("eml")
          .withMetadataNamespace("eml://ecoinformatics.org/eml-2.1.1")
          .withSchema("http://rs.gbif.org/schema/eml-gbif-profile/1.0.2/eml.xsd");

  public OaipmhTestSteps(@LocalServerPort int port) {
    baseUrl = String.format(BASE_URL_FORMAT, port);
    OAIClient oaiClient = new HttpOAIClient(baseUrl);
    Context context =
        new Context()
            .withOAIClient(oaiClient)
            .withMetadataTransformer(
                EML_FORMAT.getMetadataPrefix(),
                org.dspace.xoai.dataprovider.model.MetadataFormat.identity());
    serviceProvider = new ServiceProvider(context);
  }

  @After("@OaipmhGetRecord")
  public void after() {
    actualException = null;
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

  @Given("Get record parameters")
  public void prepareParameters(GetRecordParameters parameters) {
    recordParameters = parameters;
  }

  @When("Get record")
  public void getRecord() {
    try {
      record = serviceProvider.getRecord(recordParameters);
      System.out.println("stuff");
    } catch (Exception e) {
      actualException = e;
    }
  }

  @Then("IdDoesNotExistException is expected")
  public void idDoesNotExistExceptionIsExpected() {
    assertThat(actualException, is(notNullValue()));
    assertThat(actualException, instanceOf(IdDoesNotExistException.class));
  }

  @Then("CannotDisseminateFormatException is expected")
  public void cannotDisseminateFormatExceptionIsExpected() {
    assertThat(actualException, is(notNullValue()));
    assertThat(actualException, instanceOf(CannotDisseminateFormatException.class));
  }
}
