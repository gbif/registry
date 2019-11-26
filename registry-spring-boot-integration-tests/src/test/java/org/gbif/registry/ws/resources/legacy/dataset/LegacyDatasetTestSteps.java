package org.gbif.registry.ws.resources.legacy.dataset;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.ws.TestEmailConfiguration;
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

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;
import java.util.Objects;

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
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@SpringBootTest(classes = {TestEmailConfiguration.class,
  RegistryIntegrationTestsConfiguration.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class LegacyDatasetTestSteps {

  private MockMvc mvc;
  private ResultActions result;
  private HttpHeaders requestParamsDataset;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private DataSource ds;

  private Connection connection;

  @Before("@LegacyDataset")
  public void setUp() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/legacydataset/legacy_dataset_cleanup.sql"));
    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/legacydataset/legacy_dataset_prepare.sql"));

    mvc = MockMvcBuilders
      .webAppContextSetup(context)
      .apply(springSecurity())
      .build();
  }

  @After("@LegacyDataset")
  public void tearDown() throws Exception {
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/legacydataset/legacy_dataset_cleanup.sql"));

    connection.close();
  }

  @Given("organization {string} with key {string}")
  public void prepareOrganization(String orgName, String orgKey) {
    // prepared by script, see @Before
  }

  @Given("installation {string} with key {string}")
  public void prepareInstallation(String instName, String installationKey) {
    // prepared by script, see @Before
  }

  @Given("dataset {string} with key {string}")
  public void prepareDataset(String name, String datasetKey) {
    // prepared by script, see @Before
  }

  @Given("dataset contact {string} with key {int}")
  public void prepareContact(String email, int key) {
    // prepared by script, see @Before
  }

  @Given("dataset endpoint {string} with key {int}")
  public void prepareEndpoint(String description, int key) {
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
    requestParamsDataset.add(PRIMARY_CONTACT_ADDRESS_PARAM, params.get(PRIMARY_CONTACT_ADDRESS_PARAM));
    requestParamsDataset.add(PRIMARY_CONTACT_PHONE_PARAM, params.get(PRIMARY_CONTACT_PHONE_PARAM));

    // endpoint(s)
    requestParamsDataset.add(SERVICE_TYPES_PARAM, params.get(SERVICE_TYPES_PARAM));
    requestParamsDataset.add(SERVICE_URLS_PARAM, params.get(SERVICE_URLS_PARAM));

    // add additional ipt and organisation parameters
    requestParamsDataset.add(IPT_KEY_PARAM, params.get(IPT_KEY_PARAM));
  }

  @When("update dataset {string} with key {string} using {word} organization key {string} and password {string}")
  public void updateIptDataset(String datasetName, String datasetKey, String valid, String orgKey,
                               String password, Map<String, String> params) throws Exception {
    requestParamsDataset.set(DESCRIPTION_PARAM, params.get(DESCRIPTION_PARAM));
    requestParamsDataset.set(NAME_PARAM, params.get(NAME_PARAM));

    result = mvc
      .perform(
        post("/registry/resource/{key}", datasetKey)
          .params(requestParamsDataset)
          .contentType(APPLICATION_FORM_URLENCODED)
          .accept(APPLICATION_XML)
          .with(httpBasic(orgKey, password)))
      .andDo(print());
  }

  @Then("response status should be {int}")
  public void checkResponseStatus(int status) throws Exception {
    result
      .andExpect(status().is(status));
  }

  @SuppressWarnings("ConstantConditions")
  @Then("registered/updated dataset is")
  public void checkRegisteredDatasetValidity(Dataset expectedDataset) throws Exception {
    result
      .andExpect(
        xpath("/resource/key")
          .string(expectedDataset.getKey().toString()))
      .andExpect(
        xpath("/resource/organisationKey")
          .string(expectedDataset.getPublishingOrganizationKey().toString()))
      .andExpect(
        xpath("/resource/publishingOrganizationKey")
          .string(expectedDataset.getPublishingOrganizationKey().toString()))
      .andExpect(
        xpath("/resource/installationKey")
          .string(expectedDataset.getInstallationKey().toString()))
      .andExpect(
        xpath("/resource/name")
          .string(expectedDataset.getTitle()))
      .andExpect(
        xpath("/resource/title")
          .string(expectedDataset.getTitle()))
      .andExpect(
        xpath("/resource/nameLanguage")
          .string(expectedDataset.getLanguage().getIso2LetterCode()))
      .andExpect(
        xpath("/resource/description")
          .string(expectedDataset.getDescription()))
      .andExpect(
        xpath("/resource/descriptionLanguage")
          .string(expectedDataset.getLanguage().getIso2LetterCode()))
      .andExpect(
        xpath("/resource/createdBy")
          .string(expectedDataset.getCreatedBy()))
      .andExpect(
        xpath("/resource/modifiedBy")
          .string(expectedDataset.getModifiedBy()))
      .andExpect(
        xpath("/resource/external")
          .booleanValue(false))
      .andExpect(
        xpath("/resource/homepage")
          .string(expectedDataset.getHomepage().toString()))
      .andExpect(
        xpath("/resource/homepageURL")
          .string(expectedDataset.getHomepage().toString()))
      .andExpect(
        xpath("/resource/language")
          .string(expectedDataset.getLanguage().toString()))
      .andExpect(
        xpath("/resource/logoUrl")
          .string(expectedDataset.getLogoUrl().toString()))
      .andExpect(
        xpath("/resource/logoURL")
          .string(expectedDataset.getLogoUrl().toString()))
      .andExpect(
        xpath("/resource/type")
          .string(expectedDataset.getType().toString()));
  }

  @SuppressWarnings("ConstantConditions")
  @Then("updated dataset contact is")
  public void checkDatasetContactKeySameAfterUpdate(Contact expectedContact) throws Exception {
    result
      .andExpect(
        xpath("/resource/contacts/key")
          .string(expectedContact.getKey().toString()))
      .andExpect(
        xpath("/resource/contacts/type")
          .string(expectedContact.getType().name()))
      .andExpect(
        xpath("/resource/contacts/primary")
          .booleanValue(expectedContact.isPrimary()))
      .andExpect(
        xpath("/resource/contacts/firstName")
          .string(expectedContact.getFirstName()))
      .andExpect(
        xpath("/resource/contacts/lastName")
          .string(expectedContact.getLastName()))
      .andExpect(
        xpath("/resource/contacts/position")
          .string(expectedContact.getPosition().get(0)))
      .andExpect(
        xpath("/resource/contacts/description")
          .string(expectedContact.getDescription()))
      .andExpect(
        xpath("/resource/contacts/email")
          .string(expectedContact.getEmail().get(0)))
      .andExpect(
        xpath("/resource/contacts/phone")
          .string(expectedContact.getPhone().get(0)))
      .andExpect(
        xpath("/resource/contacts/organization")
          .string(expectedContact.getOrganization()))
      .andExpect(
        xpath("/resource/contacts/address")
          .string(expectedContact.getAddress().get(0)))
      .andExpect(
        xpath("/resource/contacts/city")
          .string(expectedContact.getCity()))
      .andExpect(
        xpath("/resource/contacts/province")
          .string(expectedContact.getProvince()))
      .andExpect(
        xpath("/resource/contacts/country")
          .string(expectedContact.getCountry().toString()))
      .andExpect(
        xpath("/resource/contacts/postalCode")
          .string(expectedContact.getPostalCode()))
      .andExpect(
        xpath("/resource/contacts/createdBy")
          .string(expectedContact.getCreatedBy()))
      .andExpect(
        xpath("/resource/contacts/modifiedBy")
          .string(expectedContact.getModifiedBy()));
  }
}
