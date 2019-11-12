package org.gbif.registry.ws.resources.legacy.ipt;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.utils.Parsers;
import org.gbif.registry.ws.TestEmailConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.net.URI;
import java.sql.Connection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.gbif.registry.utils.LenientAssert.assertLenientEquals;
import static org.gbif.registry.ws.util.LegacyResourceConstants.DESCRIPTION_PARAM;
import static org.gbif.registry.ws.util.LegacyResourceConstants.HOMEPAGE_URL_PARAM;
import static org.gbif.registry.ws.util.LegacyResourceConstants.IPT_KEY_PARAM;
import static org.gbif.registry.ws.util.LegacyResourceConstants.LOGO_URL_PARAM;
import static org.gbif.registry.ws.util.LegacyResourceConstants.NAME_PARAM;
import static org.gbif.registry.ws.util.LegacyResourceConstants.ORGANIZATION_KEY_PARAM;
import static org.gbif.registry.ws.util.LegacyResourceConstants.PRIMARY_CONTACT_ADDRESS_PARAM;
import static org.gbif.registry.ws.util.LegacyResourceConstants.PRIMARY_CONTACT_EMAIL_PARAM;
import static org.gbif.registry.ws.util.LegacyResourceConstants.PRIMARY_CONTACT_NAME_PARAM;
import static org.gbif.registry.ws.util.LegacyResourceConstants.PRIMARY_CONTACT_PHONE_PARAM;
import static org.gbif.registry.ws.util.LegacyResourceConstants.PRIMARY_CONTACT_TYPE_PARAM;
import static org.gbif.registry.ws.util.LegacyResourceConstants.SERVICE_TYPES_PARAM;
import static org.gbif.registry.ws.util.LegacyResourceConstants.SERVICE_URLS_PARAM;
import static org.gbif.registry.ws.util.LegacyResourceConstants.WS_PASSWORD_PARAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {TestEmailConfiguration.class, RegistryIntegrationTestsConfiguration.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class IptTestSteps {

  private MockMvc mvc;
  private ResultActions result;

  private HttpHeaders requestParamsInstallation;
  private HttpHeaders requestParamsDataset;
  private Installation actualInstallation;
  private Dataset actualDataset;
  private UUID installationKey;
  private UUID datasetKey;
  private Integer contactKeyBeforeSecondUpdate;
  private Integer endpointKeyBeforeSecondUpdate;
  private Date installationCreationDate;
  private String installationCreatedBy;
  private Date datasetCreationDate;
  private String datasetCreatedBy;

  @Autowired
  private DatasetService datasetService;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private DataSource ds;

  private Connection connection;

  @Autowired
  private InstallationService installationService;

  @Before("@IPT")
  public void setUp() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/ipt/ipt_register_cleanup.sql"));
    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/ipt/ipt_register_prepare.sql"));

    mvc = MockMvcBuilders
      .webAppContextSetup(context)
      .apply(springSecurity())
      .build();
  }

  @After("@IPT")
  public void tearDown() throws Exception {
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/ipt/ipt_register_cleanup.sql"));

    connection.close();
  }

  @Given("organization {string} with key {string}")
  public void prepareOrganization(String orgName, String orgKey) {
    // prepared by script ipt_register_prepare
  }

  @Given("installation {string} with key {string}")
  public void prepareInstallation(String instName, String installationKey) {
    this.installationKey = UUID.fromString(installationKey);
    actualInstallation = installationService.get(this.installationKey);
    installationCreationDate = actualInstallation.getCreated();
    installationCreatedBy = actualInstallation.getCreatedBy();
  }

  @Given("dataset {string} with key {string}")
  public void prepareDataset(String name, String datasetKey) {
    this.datasetKey = UUID.fromString(datasetKey);
    actualDataset = datasetService.get(this.datasetKey);
    datasetCreationDate = actualDataset.getCreated();
    datasetCreatedBy = actualDataset.getCreatedBy();
  }

  @Given("query parameters for installation registration or updating")
  public void prepareRequestParamsInstallation(Map<String, String> params) {
    requestParamsInstallation = new HttpHeaders();
    // main
    requestParamsInstallation.add(ORGANIZATION_KEY_PARAM, params.get(ORGANIZATION_KEY_PARAM));
    requestParamsInstallation.add(NAME_PARAM, params.get(NAME_PARAM));
    requestParamsInstallation.add(DESCRIPTION_PARAM, params.get(DESCRIPTION_PARAM));

    // primary contact
    requestParamsInstallation.add(PRIMARY_CONTACT_TYPE_PARAM, params.get(PRIMARY_CONTACT_TYPE_PARAM));
    requestParamsInstallation.add(PRIMARY_CONTACT_NAME_PARAM, params.get(PRIMARY_CONTACT_NAME_PARAM));
    requestParamsInstallation.add(PRIMARY_CONTACT_EMAIL_PARAM, params.get(PRIMARY_CONTACT_EMAIL_PARAM));

    // service/endpoint
    requestParamsInstallation.add(SERVICE_TYPES_PARAM, params.get(SERVICE_TYPES_PARAM));
    requestParamsInstallation.add(SERVICE_URLS_PARAM, URI.create(params.get(SERVICE_URLS_PARAM)).toASCIIString());

    // add IPT password used for updating the IPT's own metadata & issuing atomic updateURL operations
    requestParamsInstallation.add(WS_PASSWORD_PARAM, params.get(WS_PASSWORD_PARAM));
  }

  @Given("query parameters to dataset registration or updating")
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
    requestParamsDataset.add(PRIMARY_CONTACT_ADDRESS_PARAM, params.get(PRIMARY_CONTACT_ADDRESS_PARAM));
    requestParamsDataset.add(PRIMARY_CONTACT_PHONE_PARAM, params.get(PRIMARY_CONTACT_PHONE_PARAM));

    // endpoint(s)
    requestParamsDataset.add(SERVICE_TYPES_PARAM, params.get(SERVICE_TYPES_PARAM));
    requestParamsDataset.add(SERVICE_URLS_PARAM, params.get(SERVICE_URLS_PARAM));

    // add additional ipt and organisation parameters
    requestParamsDataset.add(IPT_KEY_PARAM, params.get(IPT_KEY_PARAM));
  }

  @Given("dataset parameter {word} is {string}")
  public void changeParamForDataset(String paramName, String paramValue) {
    requestParamsDataset.set(paramName, paramValue);
  }

  @Given("{word} parameters without field {string}")
  public void removePrimaryContactFromParams(String type, String field) {
    if ("dataset".equals(type)) {
      requestParamsDataset.remove(field);
    } else {
      requestParamsInstallation.remove(field);
    }
  }

  @When("register new installation for organization {string} using valid/invalid organization key {string} and password {string}")
  public void registerIpt(String orgName, String organisationKey, String password) throws Exception {
    result = mvc
      .perform(
        post("/registry/ipt/register")
          .params(requestParamsInstallation)
          .contentType(APPLICATION_FORM_URLENCODED)
          .accept(APPLICATION_XML)
          .with(httpBasic(organisationKey, password)))
      .andDo(print());
  }

  @When("register new dataset using valid/invalid organization key {string} and password {string}")
  public void registerDataset(String installationKey, String password) throws Exception {
    result = mvc
      .perform(
        post("/registry/ipt/resource")
          .params(requestParamsDataset)
          .contentType(APPLICATION_FORM_URLENCODED)
          .accept(APPLICATION_XML)
          .with(httpBasic(installationKey, password))
      ).andDo(print());
  }

  @When("update installation {string} using valid/invalid installation key {string} and password {string}")
  public void updateIptInstallation(String instName, String installationKey, String password, Map<String, String> params) throws Exception {
    requestParamsInstallation.set(DESCRIPTION_PARAM, params.get(DESCRIPTION_PARAM));
    requestParamsInstallation.set(NAME_PARAM, params.get(NAME_PARAM));

    result = mvc
      .perform(
        post("/registry/ipt/update/{key}", installationKey)
          .params(requestParamsInstallation)
          .contentType(APPLICATION_FORM_URLENCODED)
          .accept(APPLICATION_XML)
          .with(httpBasic(installationKey, password)))
      .andDo(print());
  }

  @When("update dataset {string} with key {string} using valid/invalid installation key {string} and password {string}")
  public void updateIptDataset(String datasetName, String datasetKey, String installationKey, String password, Map<String, String> params) throws Exception {
    requestParamsDataset.set(DESCRIPTION_PARAM, params.get(DESCRIPTION_PARAM));
    requestParamsDataset.set(NAME_PARAM, params.get(NAME_PARAM));

    result = mvc
      .perform(
        post("/registry/ipt/resource/{key}", datasetKey)
          .params(requestParamsDataset)
          .contentType(APPLICATION_FORM_URLENCODED)
          .accept(APPLICATION_XML)
          .with(httpBasic("36107c15-771c-4810-a298-b7558828b8bd", password)))
      .andDo(print());
  }

  @Then("response status should be {int}")
  public void checkResponseStatus(int status) throws Exception {
    result
      .andExpect(status().is(status));
  }

  @Then("installation UUID is returned")
  public void checkInstallationUuid() throws Exception {
    MvcResult mvcResult = result.andReturn();
    String contentAsString = mvcResult.getResponse().getContentAsString();
    Parsers.saxParser.parse(Parsers.getUtf8Stream(contentAsString), Parsers.legacyIptEntityHandler);
    installationKey = UUID.fromString(Parsers.legacyIptEntityHandler.key);
    assertNotNull("Registered IPT key should be in response", installationKey);
  }

  @Then("dataset UUID is returned")
  public void checkDatasetUuid() throws Exception {
    MvcResult mvcResult = result.andReturn();
    String contentAsString = mvcResult.getResponse().getContentAsString();
    Parsers.saxParser.parse(Parsers.getUtf8Stream(contentAsString), Parsers.legacyIptEntityHandler);
    datasetKey = UUID.fromString(Parsers.legacyIptEntityHandler.key);
    assertNotNull("Registered Dataset key should be in response", datasetKey);
  }

  @Then("registered/updated installation is")
  public void checkRegisteredOrUpdatedInstallationValidity(Installation expectedInstallation) {
    actualInstallation = installationService.get(installationKey);
    assertLenientEquals("Installations do not match", expectedInstallation, actualInstallation);
    assertNotNull(actualInstallation.getCreated());
    assertNotNull(actualInstallation.getModified());
  }

  @Then("registered/updated {word} contacts are")
  public void checkContacts(String entityType, List<Contact> expectedContacts) {
    List<Contact> actualContacts = "dataset".equals(entityType)
      ? actualDataset.getContacts() : actualInstallation.getContacts();

    for (int i = 0; i < expectedContacts.size(); i++) {
      Contact actualContact = actualContacts.get(i);
      assertLenientEquals("Contact does not match", expectedContacts.get(i), actualContact);
      assertNotNull(actualContact.getCreatedBy());
      assertNotNull(actualContact.getModifiedBy());
    }
  }

  @Then("registered/updated {word} endpoints are")
  public void checkEndpoints(String entityType, List<Endpoint> expectedEndpoints) {
    List<Endpoint> actualEndpoints = "dataset".equals(entityType)
      ? actualDataset.getEndpoints() : actualInstallation.getEndpoints();

    for (int i = 0; i < expectedEndpoints.size(); i++) {
      Endpoint actualEndpoint = actualEndpoints.get(i);
      assertLenientEquals("Endpoint does not match", expectedEndpoints.get(i), actualEndpoint);
      assertNotNull(actualEndpoint.getCreatedBy());
      assertNotNull(actualEndpoint.getModifiedBy());
    }
  }

  @Then("registered/updated dataset is")
  public void checkRegisteredDatasetValidity(Dataset expectedDataset) {
    actualDataset = datasetService.get(datasetKey);
    copyGeneratedFieldsForDataset(expectedDataset, actualDataset);
    assertLenientEquals("Datasets do not match", expectedDataset, actualDataset);
    assertNotNull(actualDataset.getCreatedBy());
    assertNotNull(actualDataset.getModifiedBy());
  }

  private void copyGeneratedFieldsForDataset(Dataset expectedDataset, Dataset actualDataset) {
    expectedDataset.setDoi(actualDataset.getDoi());
    expectedDataset.setCitation(actualDataset.getCitation());
  }

  @Then("total number of installations is {int}")
  public void checkNumberOfInstallations(int installationsNumber) {
    assertEquals(installationsNumber, installationService.list(new PagingRequest(0, 10)).getResults().size());
  }

  @Then("created fields were not updated")
  public void checkCreatedFields() {
    assertEquals(installationCreationDate, actualInstallation.getCreated());
    assertEquals(installationCreatedBy, actualInstallation.getCreatedBy());
  }

  @Given("store contactKey and endpointKey")
  public void storeContactKeyAndEndpointKey() {
    contactKeyBeforeSecondUpdate = actualInstallation.getContacts().get(0).getKey();
    endpointKeyBeforeSecondUpdate = actualInstallation.getEndpoints().get(0).getKey();
  }

  @Then("contactKey is the same")
  public void checkContactKeySameAfterUpdate() {
    // compare contact key and make sure it doesn't change after update (Contacts are mutable)
    assertEquals(contactKeyBeforeSecondUpdate, actualInstallation.getContacts().get(0).getKey());
  }

  @Then("endpointKey was updated")
  public void checkEndpointKeyNewAfterUpdate() {
    // compare endpoint key and make sure it does change after update (Endpoints are not mutable)
    assertNotEquals(endpointKeyBeforeSecondUpdate, actualInstallation.getEndpoints().get(0).getKey());
  }
}
