package org.gbif.registry.ws.resources.legacy.ipt;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.utils.Parsers;
import org.gbif.registry.ws.TestEmailConfiguration;
import org.gbif.registry.ws.model.LegacyDataset;
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

import static java.util.Collections.singletonList;
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

  private HttpHeaders requestParams;
  private Installation actualInstallation;
  private Dataset actualDataset;
  private UUID organizationKey;
  private UUID installationKey;
  private UUID datasetKey;
  private Integer contactKeyBeforeSecondUpdate;
  private Integer endpointKeyBeforeSecondUpdate;
  private Date installationCreationDate;
  private String installationCreatedBy;

  @Autowired
  private DatasetService datasetService;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private DataSource ds;

  private Connection connection;

  @Autowired
  private OrganizationService organizationService;

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
    organizationKey = UUID.fromString(orgKey);
  }

  @Given("installation {string} with key {string}")
  public void prepareInstallation(String instName, String installationKey) {
    this.installationKey = UUID.fromString(installationKey);
    actualInstallation = installationService.get(this.installationKey);
    installationCreationDate = actualInstallation.getCreated();
    installationCreatedBy = actualInstallation.getCreatedBy();
  }

  @Given("query parameters for installation registration/updating")
  public void prepareRequestParamsInstallation(Map<String, String> params) {
    requestParams = new HttpHeaders();
    // main
    requestParams.add(ORGANIZATION_KEY_PARAM, params.get(ORGANIZATION_KEY_PARAM));
    requestParams.add(NAME_PARAM, params.get(NAME_PARAM));
    requestParams.add(DESCRIPTION_PARAM, params.get(DESCRIPTION_PARAM));

    // primary contact
    requestParams.add(PRIMARY_CONTACT_TYPE_PARAM, params.get(PRIMARY_CONTACT_TYPE_PARAM));
    requestParams.add(PRIMARY_CONTACT_NAME_PARAM, params.get(PRIMARY_CONTACT_NAME_PARAM));
    requestParams.add(PRIMARY_CONTACT_EMAIL_PARAM, params.get(PRIMARY_CONTACT_EMAIL_PARAM));

    // service/endpoint
    requestParams.add(SERVICE_TYPES_PARAM, params.get(SERVICE_TYPES_PARAM));
    requestParams.add(SERVICE_URLS_PARAM, URI.create(params.get(SERVICE_URLS_PARAM)).toASCIIString());

    // add IPT password used for updating the IPT's own metadata & issuing atomic updateURL operations
    requestParams.add(WS_PASSWORD_PARAM, params.get(WS_PASSWORD_PARAM));
  }

  @Given("query parameters to dataset registration")
  public void prepareRequestParamsDataset(Map<String, String> params) {
    requestParams = new HttpHeaders();
    // main
    requestParams.add(ORGANIZATION_KEY_PARAM, params.get(ORGANIZATION_KEY_PARAM));
    requestParams.add(NAME_PARAM, params.get(NAME_PARAM));
    requestParams.add(DESCRIPTION_PARAM, params.get(DESCRIPTION_PARAM));
    requestParams.add(HOMEPAGE_URL_PARAM, params.get(HOMEPAGE_URL_PARAM));
    requestParams.add(LOGO_URL_PARAM, params.get(LOGO_URL_PARAM));

    // primary contact
    requestParams.add(PRIMARY_CONTACT_TYPE_PARAM, params.get(PRIMARY_CONTACT_TYPE_PARAM));
    requestParams.add(PRIMARY_CONTACT_EMAIL_PARAM, params.get(PRIMARY_CONTACT_EMAIL_PARAM));
    requestParams.add(PRIMARY_CONTACT_NAME_PARAM, params.get(PRIMARY_CONTACT_NAME_PARAM));
    requestParams.add(PRIMARY_CONTACT_ADDRESS_PARAM, params.get(PRIMARY_CONTACT_ADDRESS_PARAM));
    requestParams.add(PRIMARY_CONTACT_PHONE_PARAM, params.get(PRIMARY_CONTACT_PHONE_PARAM));

    // endpoint(s)
    requestParams.add(SERVICE_TYPES_PARAM, params.get(SERVICE_TYPES_PARAM));
    requestParams.add(SERVICE_URLS_PARAM, params.get(SERVICE_URLS_PARAM));

    // add additional ipt and organisation parameters
    requestParams.add(IPT_KEY_PARAM, params.get(IPT_KEY_PARAM));
  }

  @Given("without field {string}")
  public void removePrimaryContactFromParams(String field) {
    requestParams.remove(field);
  }

  @When("register new installation for organization {string} using organization key {string} and password {string}")
  public void registerIpt(String orgName, String organisationKey, String password) throws Exception {
    result = mvc
      .perform(
        post("/registry/ipt/register")
          .params(requestParams)
          .contentType(APPLICATION_FORM_URLENCODED)
          .accept(APPLICATION_XML)
          .with(httpBasic(organisationKey, password)))
      .andDo(print());
  }

  @When("register new dataset using organization key {string} and password {string}")
  public void registerDataset(String installationKey, String password) throws Exception {
    result = mvc
      .perform(
        post("/registry/ipt/resource")
          .params(requestParams)
          .contentType(APPLICATION_FORM_URLENCODED)
          .accept(APPLICATION_XML)
          .with(httpBasic(installationKey, password))
      ).andDo(print());
  }

  @When("update installation {string} using installation key {string} and password {string}")
  public void updateIpt(String instName, String installationKey, String password, Map<String, String> params) throws Exception {
    requestParams.replace("description", singletonList(params.get("description")));
    requestParams.replace("name", singletonList(params.get("name")));

    result = mvc
      .perform(
        post("/registry/ipt/update/{key}", installationKey)
          .params(requestParams)
          .contentType(APPLICATION_FORM_URLENCODED)
          .accept(APPLICATION_XML)
          .with(httpBasic(installationKey, password)))
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

  @Then("registered/updated installation contacts are")
  public void checkInstallationContacts(List<Contact> expectedContacts) {
    for (int i = 0; i < expectedContacts.size(); i++) {
      Contact actualContact = actualInstallation.getContacts().get(i);
      assertLenientEquals("Contact does not match", expectedContacts.get(i), actualContact);
      assertNotNull(actualContact.getCreatedBy());
      assertNotNull(actualContact.getModifiedBy());
    }
  }

  @Then("registered/updated installation endpoints are")
  public void checkInstallationEndpoints(List<Endpoint> expectedEndpoints) {
    for (int i = 0; i < expectedEndpoints.size(); i++) {
      Endpoint actualEndpoint = actualInstallation.getEndpoints().get(i);
      assertLenientEquals("Endpoint does not match", expectedEndpoints.get(i), actualEndpoint);
      assertNotNull(actualEndpoint.getCreatedBy());
      assertNotNull(actualEndpoint.getModifiedBy());
    }
  }

  @Then("registered dataset is")
  public void checkRegisteredDatasetValidity(LegacyDataset expectedDataset) {
    actualDataset = datasetService.get(UUID.fromString(Parsers.legacyIptEntityHandler.key));
    copyGeneratedFieldsForDataset(expectedDataset, actualDataset);
    assertLenientEquals("Datasets do not match", expectedDataset, actualDataset);
    assertNotNull(actualDataset.getCreatedBy());
    assertNotNull(actualDataset.getModifiedBy());
  }

  private void copyGeneratedFieldsForDataset(Dataset expectedDataset, Dataset actualDataset) {
    expectedDataset.setDoi(actualDataset.getDoi());
    expectedDataset.setCitation(actualDataset.getCitation());
  }

  @Then("registered dataset contacts are")
  public void checkDatasetContacts(List<Contact> expectedContacts) {
    for (int i = 0; i < expectedContacts.size(); i++) {
      Contact actualContact = actualDataset.getContacts().get(i);
      assertLenientEquals("Contact does not match", expectedContacts.get(i), actualContact);
      assertNotNull(actualContact.getCreatedBy());
      assertNotNull(actualContact.getModifiedBy());
    }
  }

  @Then("registered dataset endpoints are")
  public void checkDatasetEndpoints(List<Endpoint> expectedEndpoints) {
    for (int i = 0; i < expectedEndpoints.size(); i++) {
      Endpoint actualEndpoint = actualDataset.getEndpoints().get(i);
      assertLenientEquals("Endpoint does not match", expectedEndpoints.get(i), actualEndpoint);
      assertNotNull(actualEndpoint.getCreatedBy());
      assertNotNull(actualEndpoint.getModifiedBy());
    }
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
