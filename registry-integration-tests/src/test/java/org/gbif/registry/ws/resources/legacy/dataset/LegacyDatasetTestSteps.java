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
package org.gbif.registry.ws.resources.legacy.dataset;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.domain.ws.LegacyDatasetResponse;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

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

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.DESCRIPTION_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.HOMEPAGE_URL_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.IPT_KEY_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.LOGO_URL_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.NAME_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.ORGANIZATION_KEY_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.PRIMARY_CONTACT_ADDRESS_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.PRIMARY_CONTACT_EMAIL_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.PRIMARY_CONTACT_NAME_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.PRIMARY_CONTACT_PHONE_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.PRIMARY_CONTACT_TYPE_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.SERVICE_TYPES_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.SERVICE_URLS_PARAM;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@SpringBootTest(
    classes = {RegistryIntegrationTestsConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class LegacyDatasetTestSteps {

  private MockMvc mvc;
  private ResultActions result;
  private HttpHeaders requestParamsDataset;

  @Autowired private WebApplicationContext context;

  @Autowired private DataSource ds;

  private Connection connection;

  @Before("@LegacyDataset")
  public void setUp() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/legacydataset/legacy_dataset_cleanup.sql"));
    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/legacydataset/legacy_dataset_prepare.sql"));

    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @After("@LegacyDataset")
  public void tearDown() throws Exception {
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/legacydataset/legacy_dataset_cleanup.sql"));

    connection.close();
  }

  @Given("organization {string} with key {string}")
  public void prepareOrganization(String orgName, String orgKey) {
    // prepared by script, see @Before
  }

  @Given("installation {string} with key {string} for organization {string}")
  public void prepareInstallation(String instName, String installationKey, String orgName) {
    // prepared by script, see @Before
  }

  @Given("dataset {string} with key {string} for installation {string}")
  public void prepareDataset(String name, String datasetKey, String installationName) {
    // prepared by script, see @Before
  }

  @Given("dataset contact {string} with key {int} for dataset {string}")
  public void prepareContact(String email, int key, String datasetName) {
    // prepared by script, see @Before
  }

  @Given("dataset endpoint {string} with key {int} for dataset {string}")
  public void prepareEndpoint(String description, int key, String datasetName) {
    // prepared by script, see @Before
  }

  @Given("query parameters to dataset updating")
  public void prepareRequestParamsDataset(Map<String, String> params) {
    requestParamsDataset = new HttpHeaders();
    // main
    requestParamsDataset.add(ORGANIZATION_KEY_PARAM, params.get(ORGANIZATION_KEY_PARAM));
    requestParamsDataset.add(NAME_PARAM, params.get(NAME_PARAM));
    requestParamsDataset.add(DESCRIPTION_PARAM, params.get(DESCRIPTION_PARAM));
    requestParamsDataset.add(HOMEPAGE_URL_PARAM, params.get(HOMEPAGE_URL_PARAM));
    requestParamsDataset.add(LOGO_URL_PARAM, params.get(LOGO_URL_PARAM));

    // primary contact
    requestParamsDataset.add(PRIMARY_CONTACT_TYPE_PARAM, params.get(PRIMARY_CONTACT_TYPE_PARAM));
    requestParamsDataset.add(PRIMARY_CONTACT_EMAIL_PARAM, params.get(PRIMARY_CONTACT_EMAIL_PARAM));
    requestParamsDataset.add(PRIMARY_CONTACT_NAME_PARAM, params.get(PRIMARY_CONTACT_NAME_PARAM));
    requestParamsDataset.add(
        PRIMARY_CONTACT_ADDRESS_PARAM, params.get(PRIMARY_CONTACT_ADDRESS_PARAM));
    requestParamsDataset.add(PRIMARY_CONTACT_PHONE_PARAM, params.get(PRIMARY_CONTACT_PHONE_PARAM));

    // endpoint(s)
    requestParamsDataset.add(SERVICE_TYPES_PARAM, params.get(SERVICE_TYPES_PARAM));
    requestParamsDataset.add(SERVICE_URLS_PARAM, params.get(SERVICE_URLS_PARAM));

    // add additional ipt and organisation parameters
    requestParamsDataset.add(IPT_KEY_PARAM, params.get(IPT_KEY_PARAM));
  }

  @When(
      "update dataset {string} with key {string} using {word} organization key {string} and password {string}")
  public void updateDataset(
      String datasetName,
      String datasetKey,
      String valid,
      String orgKey,
      String password,
      Map<String, String> params)
      throws Exception {
    requestParamsDataset.set(DESCRIPTION_PARAM, params.get(DESCRIPTION_PARAM));
    requestParamsDataset.set(NAME_PARAM, params.get(NAME_PARAM));

    result =
        mvc.perform(
                post("/registry/resource/{key}", datasetKey)
                    .params(requestParamsDataset)
                    .contentType(APPLICATION_FORM_URLENCODED)
                    .accept(APPLICATION_XML)
                    .with(httpBasic(orgKey, password)))
            .andDo(print());
  }

  @When(
      "perform get datasets for organization request with extension {string} and parameter {word} and value {string}")
  public void getDatasetsForOrganization(String extension, String paramName, String paramValue)
      throws Exception {
    result =
        mvc.perform(
                get("/registry/resource{extension}", extension)
                    .param(paramName, paramValue)
                    .contentType(TEXT_PLAIN))
            .andDo(print());
  }

  @When("perform get dataset {string} request with extension {string}")
  public void getDataset(String datasetKey, String extension) throws Exception {
    result =
        mvc.perform(
                get("/registry/resource/{key}{extension}", datasetKey, extension)
                    .contentType(TEXT_PLAIN))
            .andDo(print());
  }

  @Then("response status should be {int}")
  public void checkResponseStatus(int status) throws Exception {
    result.andExpect(status().is(status));
  }

  @SuppressWarnings("ConstantConditions")
  @Then("registered/updated dataset is")
  public void checkRegisteredDatasetValidity(Dataset expectedDataset) throws Exception {
    result
        .andExpect(xpath("/resource/key").string(expectedDataset.getKey().toString()))
        .andExpect(
            xpath("/resource/organisationKey")
                .string(expectedDataset.getPublishingOrganizationKey().toString()))
        .andExpect(
            xpath("/resource/publishingOrganizationKey")
                .string(expectedDataset.getPublishingOrganizationKey().toString()))
        .andExpect(
            xpath("/resource/installationKey")
                .string(expectedDataset.getInstallationKey().toString()))
        .andExpect(xpath("/resource/name").string(expectedDataset.getTitle()))
        .andExpect(xpath("/resource/title").string(expectedDataset.getTitle()))
        .andExpect(
            xpath("/resource/nameLanguage")
                .string(expectedDataset.getLanguage().getIso2LetterCode()))
        .andExpect(xpath("/resource/description").string(expectedDataset.getDescription()))
        .andExpect(
            xpath("/resource/descriptionLanguage")
                .string(expectedDataset.getLanguage().getIso2LetterCode()))
        .andExpect(xpath("/resource/createdBy").string(expectedDataset.getCreatedBy()))
        .andExpect(xpath("/resource/modifiedBy").string(expectedDataset.getModifiedBy()))
        .andExpect(xpath("/resource/external").booleanValue(false))
        .andExpect(xpath("/resource/homepage").string(expectedDataset.getHomepage().toString()))
        .andExpect(xpath("/resource/homepageURL").string(expectedDataset.getHomepage().toString()))
        .andExpect(xpath("/resource/language").string(expectedDataset.getLanguage().toString()))
        .andExpect(xpath("/resource/logoUrl").string(expectedDataset.getLogoUrl().toString()))
        .andExpect(xpath("/resource/logoURL").string(expectedDataset.getLogoUrl().toString()))
        .andExpect(xpath("/resource/type").string(expectedDataset.getType().toString()));
  }

  @SuppressWarnings("ConstantConditions")
  @Then("updated dataset contact is")
  public void checkDatasetContactKeySameAfterUpdate(Contact expectedContact) throws Exception {
    result
        .andExpect(xpath("/resource/contacts/key").string(expectedContact.getKey().toString()))
        .andExpect(xpath("/resource/contacts/type").string(expectedContact.getType().name()))
        .andExpect(xpath("/resource/contacts/primary").booleanValue(expectedContact.isPrimary()))
        .andExpect(xpath("/resource/contacts/firstName").string(expectedContact.getFirstName()))
        .andExpect(xpath("/resource/contacts/lastName").string(expectedContact.getLastName()))
        .andExpect(
            xpath("/resource/contacts/position").string(expectedContact.getPosition().get(0)))
        .andExpect(xpath("/resource/contacts/description").string(expectedContact.getDescription()))
        .andExpect(xpath("/resource/contacts/email").string(expectedContact.getEmail().get(0)))
        .andExpect(xpath("/resource/contacts/phone").string(expectedContact.getPhone().get(0)))
        .andExpect(
            xpath("/resource/contacts/organization").string(expectedContact.getOrganization()))
        .andExpect(xpath("/resource/contacts/address").string(expectedContact.getAddress().get(0)))
        .andExpect(xpath("/resource/contacts/city").string(expectedContact.getCity()))
        .andExpect(xpath("/resource/contacts/province").string(expectedContact.getProvince()))
        .andExpect(
            xpath("/resource/contacts/country").string(expectedContact.getCountry().toString()))
        .andExpect(xpath("/resource/contacts/postalCode").string(expectedContact.getPostalCode()))
        .andExpect(xpath("/resource/contacts/createdBy").string(expectedContact.getCreatedBy()))
        .andExpect(xpath("/resource/contacts/modifiedBy").string(expectedContact.getModifiedBy()));
  }

  @Then("returned {word} datasets for organization are")
  public void checkDatasetsForOrganizationResponse(
      String type, List<LegacyDatasetResponse> expectedResponse) throws Exception {
    if ("JSON".equals(type)) {
      checkDatasetsForOrganizationResponseJson(expectedResponse);
    } else {
      checkDatasetsForOrganizationResponseXml(expectedResponse);
    }
  }

  private void checkDatasetsForOrganizationResponseXml(List<LegacyDatasetResponse> expectedResponse)
      throws Exception {
    for (int i = 0; i < expectedResponse.size(); i++) {
      result
          .andExpect(
              xpath(String.format("/legacyDatasetResponses/resource[%d]/key", i + 1))
                  .string(expectedResponse.get(i).getKey()))
          .andExpect(
              xpath(String.format("/legacyDatasetResponses/resource[%d]/description", i + 1))
                  .string(expectedResponse.get(i).getDescription()))
          .andExpect(
              xpath(
                      String.format(
                          "/legacyDatasetResponses/resource[%d]/descriptionLanguage", i + 1))
                  .string(expectedResponse.get(i).getDescriptionLanguage()))
          .andExpect(
              xpath(String.format("/legacyDatasetResponses/resource[%d]/homepageURL", i + 1))
                  .string(expectedResponse.get(i).getHomepageURL()))
          .andExpect(
              xpath(String.format("/legacyDatasetResponses/resource[%d]/name", i + 1))
                  .string(expectedResponse.get(i).getName()))
          .andExpect(
              xpath(String.format("/legacyDatasetResponses/resource[%d]/nameLanguage", i + 1))
                  .string(expectedResponse.get(i).getNameLanguage()))
          .andExpect(
              xpath(String.format("/legacyDatasetResponses/resource[%d]/organisationKey", i + 1))
                  .string(expectedResponse.get(i).getOrganisationKey()));
    }
  }

  private void checkDatasetsForOrganizationResponseJson(
      List<LegacyDatasetResponse> expectedResponse) throws Exception {
    for (int i = 0; i < expectedResponse.size(); i++) {
      result
          .andExpect(jsonPath(String.format("[%d].key", i)).value(expectedResponse.get(i).getKey()))
          .andExpect(
              jsonPath(String.format("[%d].organisationKey", i))
                  .value(expectedResponse.get(i).getOrganisationKey()))
          .andExpect(
              jsonPath(String.format("[%d].name", i)).value(expectedResponse.get(i).getName()))
          .andExpect(
              jsonPath(String.format("[%d].nameLanguage", i))
                  .value(expectedResponse.get(i).getNameLanguage()))
          .andExpect(
              jsonPath(String.format("[%d].description", i))
                  .value(expectedResponse.get(i).getDescription()))
          .andExpect(
              jsonPath(String.format("[%d].descriptionLanguage", i))
                  .value(expectedResponse.get(i).getDescriptionLanguage()))
          .andExpect(
              jsonPath(String.format("[%d].homepageURL", i))
                  .value(expectedResponse.get(i).getHomepageURL()));
    }
  }

  @Then("datasets error {word} response is {string}")
  public void checkDatasetsForOrganizationErrorResponse(String type, String expectedErrorMessage)
      throws Exception {
    if ("JSON".equals(type)) {
      result.andExpect(jsonPath("$.error").value(expectedErrorMessage));
    } else {
      result.andExpect(xpath("/IptError/@error").string(expectedErrorMessage));
    }
  }

  @Then("datasets {word} response is empty")
  public void checkDatasetsForOrganizationEmptyResponse(String type) throws Exception {
    if ("JSON".equals(type)) {
      result.andExpect(jsonPath("$").isEmpty());
    } else {
      result.andExpect(xpath("/legacyDatasetResponses").string(""));
    }
  }

  @Then("dataset response for case {word} is")
  public void checkDataset(String type, LegacyDatasetResponse expectedDataset) throws Exception {
    if ("JSON".equals(type)) {
      checkDatasetResponseJson(expectedDataset);
    } else {
      checkDatasetResponseXml(expectedDataset);
      ;
    }
  }

  private void checkDatasetResponseXml(LegacyDatasetResponse expectedDataset) throws Exception {
    result
        .andExpect(xpath("/resource/key").string(expectedDataset.getKey()))
        .andExpect(xpath("/resource/description").string(expectedDataset.getDescription()))
        .andExpect(
            xpath("/resource/descriptionLanguage").string(expectedDataset.getDescriptionLanguage()))
        .andExpect(xpath("/resource/homepageURL").string(expectedDataset.getHomepageURL()))
        .andExpect(xpath("/resource/name").string(expectedDataset.getName()))
        .andExpect(xpath("/resource/nameLanguage").string(expectedDataset.getNameLanguage()))
        .andExpect(xpath("/resource/organisationKey").string(expectedDataset.getOrganisationKey()))
        .andExpect(
            xpath("/resource/primaryContactAddress")
                .string(expectedDataset.getPrimaryContactAddress()))
        .andExpect(
            xpath("/resource/primaryContactDescription")
                .string(expectedDataset.getPrimaryContactDescription()))
        .andExpect(
            xpath("/resource/primaryContactEmail").string(expectedDataset.getPrimaryContactEmail()))
        .andExpect(
            xpath("/resource/primaryContactName").string(expectedDataset.getPrimaryContactName()))
        .andExpect(
            xpath("/resource/primaryContactPhone").string(expectedDataset.getPrimaryContactPhone()))
        .andExpect(
            xpath("/resource/primaryContactType").string(expectedDataset.getPrimaryContactType()));
  }

  private void checkDatasetResponseJson(LegacyDatasetResponse expectedDataset) throws Exception {
    result
        .andExpect(jsonPath("$.key").value(expectedDataset.getKey()))
        .andExpect(jsonPath("$.organisationKey").value(expectedDataset.getOrganisationKey()))
        .andExpect(jsonPath("$.name").value(expectedDataset.getName()))
        .andExpect(jsonPath("$.nameLanguage").value(expectedDataset.getNameLanguage()))
        .andExpect(jsonPath("$.description").value(expectedDataset.getDescription()))
        .andExpect(
            jsonPath("$.descriptionLanguage").value(expectedDataset.getDescriptionLanguage()))
        .andExpect(jsonPath("$.homepageURL").value(expectedDataset.getHomepageURL()))
        .andExpect(jsonPath("$.primaryContactName").value(expectedDataset.getPrimaryContactName()))
        .andExpect(
            jsonPath("$.primaryContactAddress").value(expectedDataset.getPrimaryContactAddress()))
        .andExpect(
            jsonPath("$.primaryContactEmail").value(expectedDataset.getPrimaryContactEmail()))
        .andExpect(
            jsonPath("$.primaryContactPhone").value(expectedDataset.getPrimaryContactPhone()))
        .andExpect(
            jsonPath("$.primaryContactDescription")
                .value(expectedDataset.getPrimaryContactDescription()))
        .andExpect(jsonPath("$.primaryContactType").value(expectedDataset.getPrimaryContactType()));
  }

  @SuppressWarnings("ConstantConditions")
  @Then("content type {string} is expected")
  public void checkResponseProperFormatAndParse(String contentType) {
    assertTrue(result.andReturn().getResponse().getContentType().contains(contentType));
  }

  @Then("dataset error {word} response is {string}")
  public void checkErrorResponse(String type, String expectedErrorMessage) throws Exception {
    if ("JSON".equals(type)) {
      result.andExpect(jsonPath("$.error").value(expectedErrorMessage));
    } else {
      result.andExpect(xpath("/IptError/@error").string(expectedErrorMessage));
    }
  }
}
