package org.gbif.registry.ws.resources.legacy.ipt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.utils.Parsers;
import org.gbif.registry.ws.TestEmailConfiguration;
import org.gbif.registry.ws.util.LegacyResourceConstants;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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

  // set of HTTP form parameters sent in POST request
  private static final String IPT_NAME = "Test IPT Registry2";
  private static final String IPT_DESCRIPTION = "Description of Test IPT";
  private static final String IPT_PRIMARY_CONTACT_TYPE = "technical";
  private static final String IPT_PRIMARY_CONTACT_NAME = "Kyle Braak";
  private static final List<String> IPT_PRIMARY_CONTACT_EMAIL = Lists.newArrayList("kbraak@gbif.org");
  private static final String IPT_SERVICE_TYPE = "RSS";
  private static final URI IPT_SERVICE_URL = URI.create("http://ipt.gbif.org/rss.do");
  private static final String IPT_WS_PASSWORD = "welcome";

  private MockMvc mvc;
  private ResultActions result;

  private Organization organization;
  private UUID organizationKey;

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
    organization = organizationService.get(organizationKey);
  }

  @When("register new installation for organization {string}")
  public void name(String orgName) throws Exception {
    HttpHeaders headers = buildIPTParameters(organizationKey);

    // TODO in security context should be BasicUserPrincipal with UUID username (orgKey?). Now when it comes to AppIdentity it doesn't have UserPrincipal
    result = mvc
      .perform(
        post("/registry/ipt/register")
          .params(headers)
          .contentType(APPLICATION_FORM_URLENCODED)
          .accept(APPLICATION_XML)
          .with(httpBasic(organizationKey.toString(), "welcome")))
      .andDo(print());
  }

  @Then("response status should be {int}")
  public void checkResponseStatus(int status) throws Exception {
    result
      .andExpect(status().is(status));
  }

  @Then("installation UUID is returned")
  public void checkInstallationValidity() throws Exception {
    MvcResult mvcResult = result.andReturn();
    String contentAsString = mvcResult.getResponse().getContentAsString();
    Parsers.saxParser.parse(Parsers.getUtf8Stream(contentAsString), Parsers.legacyIptEntityHandler);
    assertNotNull("Registered IPT key should be in response", UUID.fromString(Parsers.legacyIptEntityHandler.key));
  }

  @Then("installation is valid")
  public void checkDates() {
    validatePersistedIptInstallation(UUID.fromString(Parsers.legacyIptEntityHandler.key), organizationKey);
  }

  /**
   * Populate a list of name value pairs used in the common ws requests for IPT registrations and updates.
   * </br>
   * Basically a copy of the method in the IPT, to ensure the parameter names are identical.
   *
   * @param organizationKey organization key (UUID)
   * @return list of name value pairs, or an empty list if the IPT or organisation key were null
   */
  private HttpHeaders buildIPTParameters(UUID organizationKey) {
    HttpHeaders httpHeaders = new HttpHeaders();
    // main
    httpHeaders.put(LegacyResourceConstants.ORGANIZATION_KEY_PARAM, Collections.singletonList(organizationKey.toString()));
    httpHeaders.put(LegacyResourceConstants.NAME_PARAM, Collections.singletonList(IPT_NAME));
    httpHeaders.put(LegacyResourceConstants.DESCRIPTION_PARAM, Collections.singletonList(IPT_DESCRIPTION));

    // primary contact
    httpHeaders.put(LegacyResourceConstants.PRIMARY_CONTACT_TYPE_PARAM, Collections.singletonList(IPT_PRIMARY_CONTACT_TYPE));
    httpHeaders.put(LegacyResourceConstants.PRIMARY_CONTACT_NAME_PARAM, Collections.singletonList(IPT_PRIMARY_CONTACT_NAME));
    httpHeaders.put(LegacyResourceConstants.PRIMARY_CONTACT_EMAIL_PARAM, IPT_PRIMARY_CONTACT_EMAIL);

    // service/endpoint
    httpHeaders.put(LegacyResourceConstants.SERVICE_TYPES_PARAM, Collections.singletonList(IPT_SERVICE_TYPE));
    httpHeaders.put(LegacyResourceConstants.SERVICE_URLS_PARAM, Collections.singletonList(IPT_SERVICE_URL.toASCIIString()));

    // add IPT password used for updating the IPT's own metadata & issuing atomic updateURL operations
    httpHeaders.put(LegacyResourceConstants.WS_PASSWORD_PARAM, Collections.singletonList(IPT_WS_PASSWORD));

    return httpHeaders;
  }

  // TODO: 30/10/2019 use lenientEquals somehow?
  /**
   * Retrieve persisted IPT installation, and make a series of assertions to ensure it has been properly persisted.
   *
   * @param installationKey installation key (UUID)
   * @param organizationKey installation hosting organization key
   * @return validated installation
   */
  private Installation validatePersistedIptInstallation(UUID installationKey, UUID organizationKey) {
    // retrieve installation anew
    Installation installation = installationService.get(installationKey);

    assertNotNull("Installation should be present", installation);
    assertEquals(organizationKey, installation.getOrganizationKey());
    assertEquals(InstallationType.IPT_INSTALLATION, installation.getType());
    assertEquals(IPT_NAME, installation.getTitle());
    assertEquals(IPT_DESCRIPTION, installation.getDescription());
    assertNotNull(installation.getCreated());
    assertNotNull(installation.getModified());

    // check installation's primary contact was properly persisted
    Contact contact = installation.getContacts().get(0);
    assertNotNull("Installation primary contact should be present", contact);
    assertNotNull(contact.getKey());
    assertTrue(contact.isPrimary());
    assertEquals(IPT_PRIMARY_CONTACT_NAME, contact.getFirstName());
    assertEquals(IPT_PRIMARY_CONTACT_EMAIL, contact.getEmail());
    assertEquals(ContactType.TECHNICAL_POINT_OF_CONTACT, contact.getType());
    assertNotNull(contact.getCreated());
    assertNotNull(contact.getCreatedBy());
    assertNotNull(contact.getModified());
    assertNotNull(contact.getModifiedBy());

    // check installation's RSS/FEED endpoint was properly persisted
    Endpoint endpoint = installation.getEndpoints().get(0);
    assertNotNull("Installation FEED endpoint should be present", endpoint);
    assertNotNull(endpoint.getKey());
    assertEquals(IPT_SERVICE_URL, endpoint.getUrl());
    assertEquals(EndpointType.FEED, endpoint.getType());
    assertNotNull(endpoint.getCreated());
    assertNotNull(endpoint.getCreatedBy());
    assertNotNull(endpoint.getModified());
    assertNotNull(endpoint.getModifiedBy());

    return installation;
  }
}
