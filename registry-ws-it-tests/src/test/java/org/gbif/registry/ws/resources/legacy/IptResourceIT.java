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
package org.gbif.registry.ws.resources.legacy;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.domain.ws.IptEntityResponse;
import org.gbif.registry.test.Datasets;
import org.gbif.registry.test.Organizations;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.utils.Requests;
import org.gbif.registry.ws.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.ws.fixtures.RequestTestFixture;
import org.gbif.registry.ws.fixtures.TestConstants;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.Md5EncodeService;
import org.gbif.ws.security.SigningService;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.collect.Lists;

import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;

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
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.WS_PASSWORD_PARAM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RegistryIntegrationTestsConfiguration.class)
@ContextConfiguration(initializers = {IptResourceIT.ContextInitializer.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class IptResourceIT {

  @RegisterExtension
  static PreparedDbExtension database =
      EmbeddedPostgresExtension.preparedDatabase(
          LiquibasePreparer.forClasspathLocation("liquibase/master.xml"));

  @RegisterExtension
  public final DatabaseInitializer databaseRule =
      new DatabaseInitializer(database.getTestDatabase());

  static class ContextInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of(dbTestPropertyPairs())
          .applyTo(configurableApplicationContext.getEnvironment());
      withSearchEnabled(false, configurableApplicationContext.getEnvironment());
    }

    protected static void withSearchEnabled(
        boolean enabled, ConfigurableEnvironment configurableEnvironment) {
      TestPropertyValues.of("searchEnabled=" + enabled).applyTo(configurableEnvironment);
    }

    protected String[] dbTestPropertyPairs() {
      return new String[] {
        "registry.datasource.url=jdbc:postgresql://localhost:"
            + database.getConnectionInfo().getPort()
            + "/"
            + database.getConnectionInfo().getDbName(),
        "registry.datasource.username=" + database.getConnectionInfo().getUser(),
        "registry.datasource.password="
      };
    }
  }

  private final InstallationService installationService;
  private final DatasetService datasetService;
  private final TestDataFactory testDataFactory;
  private final RequestTestFixture requestTestFixture;
  private final SimplePrincipalProvider pp;

  // set of HTTP form parameters sent in POST request
  private static final String IPT_NAME = "Test IPT Registry2";
  private static final String IPT_DESCRIPTION = "Description of Test IPT";
  private static final String IPT_PRIMARY_CONTACT_TYPE = "technical";
  private static final String IPT_PRIMARY_CONTACT_NAME = "Kyle Braak";
  private static final List<String> IPT_PRIMARY_CONTACT_EMAIL =
      Lists.newArrayList("kbraak@gbif.org");
  private static final String IPT_SERVICE_TYPE = "RSS";
  private static final URI IPT_SERVICE_URL = URI.create("http://ipt.gbif.org/rss.do");
  private static final String IPT_WS_PASSWORD = "password";

  private static final String DATASET_SERVICE_TYPES = "EML|DWC-ARCHIVE-OCCURRENCE";
  private static final String DATASET_EVENT_SERVICE_TYPES = "EML|DWC-ARCHIVE-SAMPLING-EVENT";
  private static final String DATASET_SERVICE_URLS =
      "http://ipt.gbif.org/eml.do?r=ds123|http://ipt.gbif.org/archive.do?r=ds123";
  private static final URI DATASET_EML_SERVICE_URL =
      URI.create("http://ipt.gbif.org/eml.do?r=ds123");
  private static final URI DATASET_OCCURRENCE_SERVICE_URL =
      URI.create("http://ipt.gbif.org/archive.do?r=ds123");

  @Autowired
  public IptResourceIT(
      MockMvc mvc,
      SigningService signingService,
      Md5EncodeService md5EncodeService,
      @Qualifier("registryObjectMapper") ObjectMapper objectMapper,
      XmlMapper xmlMapper,
      InstallationService installationService,
      DatasetService datasetService,
      TestDataFactory testDataFactory,
      SimplePrincipalProvider pp) {
    this.installationService = installationService;
    this.datasetService = datasetService;
    this.testDataFactory = testDataFactory;
    this.requestTestFixture =
        new RequestTestFixture(mvc, signingService, md5EncodeService, objectMapper, xmlMapper);
    this.pp = pp;
  }

  @BeforeEach
  public void setup() {
    if (pp != null) {
      pp.setPrincipal(TestConstants.TEST_ADMIN);
      SecurityContext ctx = SecurityContextHolder.createEmptyContext();
      SecurityContextHolder.setContext(ctx);
      ctx.setAuthentication(
          new UsernamePasswordAuthenticationToken(
              pp.get().getName(),
              "",
              Collections.singleton(new SimpleGrantedAuthority(UserRole.REGISTRY_ADMIN.name()))));
    }
  }

  /**
   * The test begins by persisting a new Organization. </br> Then, it sends a register IPT (POST)
   * request to create a new Installation associated to this organization. The request is sent in
   * exactly the same way as the IPT would send it, using the URL path (/ipt/register), URL encoded
   * form parameters, and basic authentication. The web service authorizes the request, and then
   * persists the Installation, associated to the Organization. </br> Upon receiving an HTTP
   * Response, the test parses its XML content in order to extract the registered IPT UUID for
   * example. </br> Last, the test validates that the installation was persisted correctly.
   */
  @Test
  public void testRegisterIpt() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    UUID organizationKey = organization.getKey();
    assertNotNull(organizationKey);

    // populate params for ws
    MultiValueMap<String, String> data = buildIptParameters(organizationKey);

    // construct request uri
    String uri = "/registry/ipt/register";

    // send POST request with credentials and check response code
    ResultActions actions =
        requestTestFixture
            .postRequestUrlEncoded(data, organizationKey, organization.getPassword(), uri)
            .andExpect(status().isCreated());

    // parse newly registered IPT key (UUID)
    IptEntityResponse iptEntityResponse =
        requestTestFixture.extractXmlResponse(actions, IptEntityResponse.class);

    assertNotNull(iptEntityResponse.getKey(), "Registered IPT key should be in response");

    // some information that should have been updated
    Installation installation =
        validatePersistedIptInstallation(
            UUID.fromString(iptEntityResponse.getKey()), organizationKey);

    // some additional information to check
    assertNotNull(installation.getCreatedBy());
    assertNotNull(installation.getModifiedBy());
  }

  /**
   * The test begins by persisting a new Organization, and Installation associated to the
   * Organization. </br> Then, it sends an update IPT (POST) request to update the same
   * Installation. The request is sent in exactly the same way as the IPT would send it, using the
   * URL path (/ipt/update/{key}), URL encoded form parameters, and basic authentication. The web
   * service authorizes the request, and then persists the Installation, updating its information.
   * </br> Upon receiving an HTTP Response, the test parses its XML content in order to extract the
   * registered IPT UUID for example. </br> Next, the test validates that the Installation's
   * information was updated correctly. The same request is then resent once more, and the test
   * validates that no duplicate installation, contact, or endpoint was created.
   */
  @Test
  public void testUpdateIpt() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    UUID organizationKey = organization.getKey();
    assertNotNull(organizationKey);

    // persist new installation of type IPT
    Installation installation = testDataFactory.newPersistedInstallation(organizationKey);
    UUID installationKey = installation.getKey();
    assertNotNull(installationKey);

    // validate it
    validateExistingIptInstallation(installation, organizationKey);

    // some information never going to change
    Date created = installation.getCreated();
    assertNotNull(created);
    String createdBy = installation.getCreatedBy();
    assertNotNull(createdBy);

    // populate params for ws
    MultiValueMap<String, String> data = buildIptParameters(organizationKey);

    // construct request uri
    String uri = "/registry/ipt/update/" + installationKey;

    // send POST request with credentials
    requestTestFixture
        .postRequestUrlEncoded(data, installationKey, installation.getPassword(), uri)
        .andExpect(status().isNoContent());

    // some information that should have been updated
    installation = validatePersistedIptInstallation(installationKey, organizationKey);

    // some additional information that should not have been updated
    assertEquals(created, installation.getCreated());
    assertEquals(createdBy, installation.getCreatedBy());

    // before sending the same POST request..
    // count the number of installations, contacts and endpoints
    assertEquals(1, installationService.list(new PagingRequest(0, 10)).getResults().size());
    assertEquals(1, installation.getContacts().size());
    assertEquals(1, installation.getEndpoints().size());
    // keep track of contact and endpoint key
    int contactKey = installation.getContacts().get(0).getKey();
    int endpointKey = installation.getEndpoints().get(0).getKey();

    // send same POST request again, to check that duplicate contact and endpoints don't get
    // persisted
    requestTestFixture
        .postRequestUrlEncoded(data, installationKey, installation.getPassword(), uri)
        .andExpect(status().isNoContent());

    // retrieve newly updated installation, and make sure the same number of installations, contacts
    // and endpoints exist
    assertEquals(1, installationService.list(new PagingRequest(0, 10)).getResults().size());
    installation = validatePersistedIptInstallation(installationKey, organizationKey);
    assertEquals(1, installation.getContacts().size());
    assertEquals(1, installation.getEndpoints().size());

    // compare contact key and make sure it doesn't change after update (Contacts are mutable)
    assertEquals(
        String.valueOf(contactKey), String.valueOf(installation.getContacts().get(0).getKey()));
    // compare endpoint key and make sure it does change after update (Endpoints are not mutable)
    assertNotEquals(
        String.valueOf(endpointKey), String.valueOf(installation.getEndpoints().get(0).getKey()));
  }

  /**
   * The test sends a update IPT (POST) request to create a new IPT, however, its organizationKey
   * HTTP Parameter doesn't match the organization key used in the credentials. The test must check
   * that the server responds with a 401 Unauthorized Response.
   */
  @Test
  public void testUpdateIptButNotAuthorized() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    UUID organizationKey = organization.getKey();
    assertNotNull(organizationKey);

    // persist new installation of type IPT
    Installation installation = testDataFactory.newPersistedInstallation(organizationKey);
    UUID installationKey = installation.getKey();
    assertNotNull(installationKey);

    // populate params for ws
    MultiValueMap<String, String> data = buildIptParameters(organizationKey);

    // construct request uri
    String uri = "/registry/ipt/update/" + installationKey;

    // send POST request with WRONG credentials
    // use the random generated key, to provoke authorization failure
    // send POST request with credentials and expect 401
    requestTestFixture
        .postRequestUrlEncoded(data, UUID.randomUUID(), installation.getPassword(), uri)
        .andExpect(status().isUnauthorized());
  }

  /**
   * The test sends an update IPT (POST) request to update an Installation, however, it is missing a
   * mandatory HTTP Parameter for the primary contact email. The test must check that the server
   * responds with a 400 BAD_REQUEST Response.
   */
  @Test
  public void testUpdateIptWithNoPrimaryContact() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    UUID organizationKey = organization.getKey();
    assertNotNull(organizationKey);

    // persist new installation of type IPT
    Installation installation = testDataFactory.newPersistedInstallation(organizationKey);
    UUID installationKey = installation.getKey();
    assertNotNull(installationKey);

    // populate params for ws
    MultiValueMap<String, String> data = buildIptParameters(organizationKey);

    assertEquals(9, data.size());
    // remove mandatory key/value before sending
    data.remove(PRIMARY_CONTACT_EMAIL_PARAM);
    assertEquals(8, data.size());

    // construct request uri
    String uri = "/registry/ipt/update/" + installationKey;

    // send POST request with credentials and expect 400
    requestTestFixture
        .postRequestUrlEncoded(data, installationKey, installation.getPassword(), uri)
        .andExpect(status().isBadRequest());
  }

  /**
   * The test sends a register IPT (POST) request to create a new Installation, however, its
   * organizationKey HTTP Parameter doesn't match the organization key used in the credentials. The
   * test must check that the server responds with a 401 Unauthorized Response.
   */
  @Test
  public void testRegisterIptButNotAuthorized() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    assertNotNull(organization.getKey());

    // populate params for ws
    MultiValueMap<String, String> data = buildIptParameters(organization.getKey());

    // construct request uri
    String uri = "/registry/ipt/register";

    // send POST request with WRONG credentials
    // use the random generated key, to provoke authorization failure and expect 401
    requestTestFixture
        .postRequestUrlEncoded(data, UUID.randomUUID(), organization.getPassword(), uri)
        .andExpect(status().isUnauthorized());
  }

  /**
   * The test sends a register IPT (POST) request to create a new Installation, however, it is
   * missing a mandatory HTTP Parameter for the primary contact email. The test must check that the
   * server responds with a 400 BAD_REQUEST Response.
   */
  @Test
  public void testRegisterIptWithNoPrimaryContact() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    UUID organizationKey = organization.getKey();
    assertNotNull(organizationKey);

    // populate params for ws
    MultiValueMap<String, String> data = buildIptParameters(organizationKey);

    assertEquals(9, data.size());
    // remove mandatory key/value before sending
    data.remove(PRIMARY_CONTACT_EMAIL_PARAM);
    assertEquals(8, data.size());

    // construct request uri
    String uri = "/registry/ipt/register";

    // send POST request with credentials and expect 400
    requestTestFixture
        .postRequestUrlEncoded(data, organizationKey, organization.getPassword(), uri)
        .andExpect(status().isBadRequest());
  }

  /**
   * The test begins by persisting a new Organization and IPT Installation. </br> Then, it sends a
   * register Dataset (POST) request to create a new Dataset owned by this organization and
   * associated to this IPT installation. The request is issued against the web services running on
   * the local Grizzly test server. The request is sent in exactly the same way as the IPT would
   * send it, using the URL path (/ipt/resource), URL encoded form parameters, and basic
   * authentication. The web service authorizes the request, and then persists the Installation,
   * associated to the Organization/Installation. </br> Upon receiving an HTTP Response, the test
   * parses its XML content in order to extract the registered Dataset UUID for example. The content
   * is parsed exactly the same way as the IPT would do it. </br> Last, the test validates that the
   * dataset was persisted correctly.
   */
  @Test
  public void testRegisterIptDataset() throws Exception {
    // persist new organization (Dataset publishing organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    UUID organizationKey = organization.getKey();
    assertNotNull(organizationKey);

    // persist new installation of type IPT
    Installation installation = testDataFactory.newPersistedInstallation(organizationKey);
    UUID installationKey = installation.getKey();
    assertNotNull(installationKey);

    // populate params for ws
    MultiValueMap<String, String> data = buildIptDatasetParameters(installationKey);
    // organisationKey param included on register, not on update
    data.add(ORGANIZATION_KEY_PARAM, organizationKey.toString());

    // construct request uri
    String uri = "/registry/ipt/resource";

    // send POST request with credentials and check response code
    ResultActions actions =
        requestTestFixture
            .postRequestUrlEncoded(data, organizationKey, organization.getPassword(), uri)
            .andExpect(status().isCreated());

    // parse newly registered IPT key (UUID)
    IptEntityResponse iptEntityResponse =
        requestTestFixture.extractXmlResponse(actions, IptEntityResponse.class);
    assertNotNull(iptEntityResponse.getKey(), "Registered Dataset key should be in response");

    // some information that should have been updated
    Dataset dataset =
        validatePersistedIptDataset(
            UUID.fromString(iptEntityResponse.getKey()),
            organizationKey,
            installationKey,
            DatasetType.OCCURRENCE);

    // some additional information to check
    assertNotNull(dataset.getCreatedBy());
    assertNotNull(dataset.getModifiedBy());
  }

  /**
   * NOTE: This test is the same as testRegisterIptDataset() except that it registers a dataset of
   * type SAMPLING_EVENT
   */
  @Test
  public void testRegisterIptEventDataset() throws Exception {
    // persist new organization (Dataset publishing organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    UUID organizationKey = organization.getKey();
    assertNotNull(organizationKey);

    // persist new installation of type IPT
    Installation installation = testDataFactory.newPersistedInstallation(organizationKey);
    UUID installationKey = installation.getKey();
    assertNotNull(installationKey);

    // populate params for ws
    MultiValueMap<String, String> data = buildIptDatasetParameters(installationKey);

    // replace service type and url params in order to reflect that this is a dataset of type
    // SAMPLING_EVENT
    data.remove(SERVICE_TYPES_PARAM);
    data.remove(SERVICE_URLS_PARAM);
    data.add(SERVICE_TYPES_PARAM, DATASET_EVENT_SERVICE_TYPES);
    data.add(SERVICE_URLS_PARAM, DATASET_SERVICE_URLS);

    // organisationKey param included on register, not on update
    data.add(ORGANIZATION_KEY_PARAM, organizationKey.toString());

    // construct request uri
    String uri = "/registry/ipt/resource";

    // send POST request with credentials and check response code
    ResultActions actions =
        requestTestFixture
            .postRequestUrlEncoded(data, organizationKey, organization.getPassword(), uri)
            .andExpect(status().isCreated());

    // parse newly registered IPT key (UUID)
    IptEntityResponse iptEntityResponse =
        requestTestFixture.extractXmlResponse(actions, IptEntityResponse.class);
    assertNotNull(iptEntityResponse.getKey(), "Registered Dataset key should be in response");

    // some information that should have been updated
    Dataset dataset =
        validatePersistedIptDataset(
            UUID.fromString(iptEntityResponse.getKey()),
            organizationKey,
            installationKey,
            DatasetType.SAMPLING_EVENT);

    // some additional information to check
    assertNotNull(dataset.getCreatedBy());
    assertNotNull(dataset.getModifiedBy());
  }

  /**
   * The test sends a register Dataset (POST) request to create a new Dataset, however, its
   * organizationKey HTTP Parameter doesn't match the organization key used in the credentials. The
   * test must check that the server responds with a 401 Unauthorized Response.
   */
  @Test
  public void testRegisterIptDatasetButNotAuthorized() throws Exception {
    // persist new organization (Dataset publishing organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    UUID organizationKey = organization.getKey();
    assertNotNull(organizationKey);

    // persist new installation of type IPT
    Installation installation = testDataFactory.newPersistedInstallation(organizationKey);
    UUID installationKey = installation.getKey();
    assertNotNull(installationKey);

    // populate params for ws
    MultiValueMap<String, String> data = buildIptDatasetParameters(installationKey);

    // organisationKey param included on register, not on update
    data.add(ORGANIZATION_KEY_PARAM, organizationKey.toString());

    // construct request uri
    String uri = "/registry/ipt/resource";

    // send POST request with WRONG credentials
    // use the random generated key, to provoke authorization failure
    requestTestFixture
        .postRequestUrlEncoded(data, UUID.randomUUID(), organization.getPassword(), uri)
        .andExpect(status().isUnauthorized());
  }

  /**
   * The test sends a register Dataset (POST) request to create a new Dataset, however, it is
   * missing a mandatory HTTP Parameter for the primary contact email. The test must check that the
   * server responds with a 400 BAD_REQUEST Response.
   */
  @Test
  public void testRegisterIptDatasetWithNoPrimaryContact() throws Exception {
    // persist new organization (Dataset publishing organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    UUID organizationKey = organization.getKey();
    assertNotNull(organizationKey);

    // persist new installation of type IPT
    Installation installation = testDataFactory.newPersistedInstallation(organizationKey);
    UUID installationKey = installation.getKey();
    assertNotNull(installationKey);

    // populate params for ws
    MultiValueMap<String, String> data = buildIptDatasetParameters(installationKey);
    // organisationKey param included on register, not on update
    data.add(ORGANIZATION_KEY_PARAM, organizationKey.toString());

    assertEquals(13, data.size());
    // remove mandatory key/value before sending
    data.remove(PRIMARY_CONTACT_TYPE_PARAM);
    assertEquals(12, data.size());

    // construct request uri
    String uri = "/registry/ipt/resource";

    // send POST request with credentials so that it passes authorization, 400 expected
    requestTestFixture
        .postRequestUrlEncoded(data, organizationKey, organization.getPassword(), uri)
        .andExpect(status().isBadRequest());
  }

  /**
   * The test begins by persisting a new Organization, Installation associated to the Organization,
   * and Dataset associated to the Organization. </br> Then, it sends an update Dataset (POST)
   * request to update the same Dataset. The request is issued against the web services running on
   * the local Grizzly test server. The request is sent in exactly the same way as the IPT would
   * send it, using the URL path (/ipt/resource/{key}), URL encoded form parameters, and basic
   * authentication. The web service authorizes the request, and then persists the Dataset, updating
   * its information. </br> Upon receiving an HTTP Response, the test parses its XML content in
   * order to extract the registered Dataset UUID for example. The content is parsed exactly the
   * same way as the IPT would do it. </br> Next, the test validates that the Dataset's information
   * was updated correctly. The same request is then resent once more, and the test validates that
   * no duplicate Dataset, contact, or endpoint was created.
   */
  @Test
  public void testUpdateIptDataset() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    UUID organizationKey = organization.getKey();
    assertNotNull(organizationKey);

    // persist new installation of type IPT
    Installation installation = testDataFactory.newPersistedInstallation(organizationKey);
    UUID installationKey = installation.getKey();
    assertNotNull(installationKey);

    // persist new Dataset associated to installation
    Dataset dataset = testDataFactory.newPersistedDataset(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();
    assertNotNull(datasetKey);

    // validate it
    validateExistingIptDataset(dataset, organizationKey, installationKey);

    // some information never going to change
    Date created = dataset.getCreated();
    assertNotNull(created);
    String createdBy = dataset.getCreatedBy();
    assertNotNull(createdBy);

    // populate params for ws
    MultiValueMap<String, String> data = buildIptDatasetParameters(installationKey);

    // construct request uri
    String uri = "/registry/ipt/resource/" + datasetKey;

    // send POST request with credentials and check response code
    requestTestFixture
        .postRequestUrlEncoded(data, organizationKey, organization.getPassword(), uri)
        .andExpect(status().isNoContent());

    // some information that should have been updated
    dataset =
        validatePersistedIptDataset(
            datasetKey, organizationKey, installationKey, DatasetType.OCCURRENCE);

    // some additional information that should not have been updated
    assertEquals(created, dataset.getCreated());
    assertEquals(createdBy, dataset.getCreatedBy());
    assertEquals(Datasets.DATASET_LANGUAGE, dataset.getLanguage());
    assertEquals(Datasets.DATASET_RIGHTS, dataset.getRights());
    assertNotNull(dataset.getCitation());
    assertEquals(Datasets.DATASET_CITATION.getIdentifier(), dataset.getCitation().getIdentifier());
    assertEquals(Datasets.DATASET_ABBREVIATION, dataset.getAbbreviation());
    assertEquals(Datasets.DATASET_ALIAS, dataset.getAlias());

    // before sending the same POST request..
    // count the number of datasets, contacts and endpoints
    assertEquals(1, datasetService.list(new PagingRequest(0, 10)).getResults().size());
    assertEquals(1, dataset.getContacts().size());
    assertEquals(2, dataset.getEndpoints().size());
    // keep track of contact and endpoint key
    int contactKey = dataset.getContacts().get(0).getKey();
    int endpointKey = dataset.getEndpoints().get(0).getKey();

    // send same POST request again, to check that duplicate contact and endpoints don't get
    // persisted
    requestTestFixture
        .postRequestUrlEncoded(data, organizationKey, organization.getPassword(), uri)
        .andExpect(status().isNoContent());

    // retrieve newly updated dataset, and make sure the same number of datasets, contacts and
    // endpoints exist
    assertEquals(1, datasetService.list(new PagingRequest(0, 10)).getResults().size());
    dataset =
        validatePersistedIptDataset(
            datasetKey, organizationKey, installationKey, DatasetType.OCCURRENCE);
    assertEquals(1, dataset.getContacts().size());
    assertEquals(2, dataset.getEndpoints().size());

    // compare contact key and make sure it doesn't change after update (Contacts are mutable)
    assertEquals(String.valueOf(contactKey), String.valueOf(dataset.getContacts().get(0).getKey()));
    // compare endpoint key and make sure it does change after update (Endpoints are not mutable)
    assertNotEquals(
        String.valueOf(endpointKey), String.valueOf(dataset.getEndpoints().get(0).getKey()));
  }

  /**
   * The test sends a update Dataset (POST) request to create a new Dataset, however, its
   * organizationKey HTTP Parameter doesn't match the organization key used in the credentials. The
   * test must check that the server responds with a 401 Unauthorized Response.
   */
  @Test
  public void testUpdateIptDatasetButNotAuthorized() throws Exception {
    // persist new organization (Dataset publishing organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    UUID organizationKey = organization.getKey();
    assertNotNull(organizationKey);

    // persist new installation of type IPT
    Installation installation = testDataFactory.newPersistedInstallation(organizationKey);
    UUID installationKey = installation.getKey();
    assertNotNull(installationKey);

    // persist new Dataset associated to installation
    Dataset dataset = testDataFactory.newPersistedDataset(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();
    assertNotNull(datasetKey);

    // populate params for ws
    MultiValueMap<String, String> data = buildIptDatasetParameters(installationKey);

    // construct request uri
    String uri = "/registry/ipt/resource/" + datasetKey;

    // send POST request with WRONG credentials
    // use the random generated key, to provoke authorization failure
    // send POST request with credentials, 401 expected
    requestTestFixture
        .postRequestUrlEncoded(data, UUID.randomUUID(), organization.getPassword(), uri)
        .andExpect(status().isUnauthorized());
  }

  /**
   * The test sends an update Dataset (POST) request to update a Dataset, however, it is missing a
   * mandatory HTTP Parameter for the primary contact email. The test must check that the server
   * responds with a 400 BAD_REQUEST Response.
   */
  @Test
  public void testUpdateIptDatasetWithNoPrimaryContact() throws Exception {
    // persist new organization (Dataset publishing organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    UUID organizationKey = organization.getKey();
    assertNotNull(organizationKey);

    // persist new installation of type IPT
    Installation installation = testDataFactory.newPersistedInstallation(organizationKey);
    UUID installationKey = installation.getKey();
    assertNotNull(installationKey);

    // persist new Dataset associated to installation
    Dataset dataset = testDataFactory.newPersistedDataset(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();
    assertNotNull(datasetKey);

    // populate params for ws
    MultiValueMap<String, String> data = buildIptDatasetParameters(installationKey);

    assertEquals(12, data.size());
    // remove mandatory key/value before sending
    data.remove(PRIMARY_CONTACT_EMAIL_PARAM);
    assertEquals(11, data.size());

    // construct request uri
    String uri = "/registry/ipt/resource/" + datasetKey;

    // send POST request with credentials, 400 expected
    requestTestFixture
        .postRequestUrlEncoded(data, organizationKey, organization.getPassword(), uri)
        .andExpect(status().isBadRequest());
  }

  /**
   * The test begins by persisting a new Organization, Installation associated to the Organization,
   * and Dataset associated to the Organization. </br> Then, it sends an update Dataset (POST)
   * request to update the same Dataset. This populates the primary contact and endpoints. </br>
   * Then, it sends a delete Dataset (POST) request to delete the Dataset. </br> Next, the test
   * validates that the Dataset was deleted correctly.
   */
  @Test
  public void testDeleteIptDataset() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    UUID organizationKey = organization.getKey();
    assertNotNull(organizationKey);

    // persist new installation of type IPT
    Installation installation = testDataFactory.newPersistedInstallation(organizationKey);
    UUID installationKey = installation.getKey();
    assertNotNull(installationKey);

    // persist new Dataset associated to installation
    Dataset dataset = testDataFactory.newPersistedDataset(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();
    assertNotNull(datasetKey);

    // construct update request uri
    String uri = "/registry/ipt/resource/" + datasetKey;

    // before sending the delete POST request, count the number of datasets, contacts and endpoints
    assertEquals(1, datasetService.list(new PagingRequest(0, 10)).getResults().size());

    // send delete POST request (using same URL), 200 expected
    requestTestFixture
        .deleteRequestUrlEncoded(organizationKey, organization.getPassword(), uri)
        .andExpect(status().isOk());

    // check that the dataset was deleted
    assertEquals(0, datasetService.list(new PagingRequest(0, 10)).getResults().size());
  }

  /**
   * Retrieve installation presumed already to exist, and make a series of assertions to ensure it
   * is valid.
   *
   * @param installation installation
   * @param organizationKey hosting organization key
   */
  private void validateExistingIptInstallation(Installation installation, UUID organizationKey) {
    assertNotNull(installation, "Installation should be present");
    assertEquals(organizationKey, installation.getOrganizationKey());
    assertEquals(InstallationType.IPT_INSTALLATION, installation.getType());
    assertNotEquals(IPT_NAME, installation.getTitle());
    assertNotEquals(IPT_DESCRIPTION, installation.getDescription());
    Date modified = installation.getModified();
    assertNotNull(modified);
    String modifiedBy = installation.getModifiedBy();
    assertNotNull(modifiedBy);
    assertTrue(installation.getContacts().isEmpty());
    assertTrue(installation.getEndpoints().isEmpty());
  }

  /**
   * Retrieve dataset presumed already to exist, and make a series of assertions to ensure it is
   * valid.
   *
   * @param dataset dataset
   * @param organizationKey publishing organization key
   * @param installationKey installation key
   */
  private void validateExistingIptDataset(
      Dataset dataset, UUID organizationKey, UUID installationKey) {
    assertNotNull(dataset, "Dataset should be present");
    assertEquals(organizationKey, dataset.getPublishingOrganizationKey());
    assertEquals(installationKey, dataset.getInstallationKey());
    assertEquals(DatasetType.OCCURRENCE, dataset.getType());
    // expected to change on update
    assertNotEquals(Requests.DATASET_NAME, dataset.getTitle());
    assertNotEquals(Requests.DATASET_DESCRIPTION, dataset.getDescription());
    assertNotEquals(Requests.DATASET_HOMEPAGE_URL, dataset.getHomepage());
    assertNotEquals(Requests.DATASET_LOGO_URL, dataset.getLogoUrl());
    Date modified = dataset.getModified();
    assertNotNull(modified);
    String modifiedBy = dataset.getModifiedBy();
    assertNotNull(modifiedBy);
    assertTrue(dataset.getContacts().isEmpty());
    assertTrue(dataset.getEndpoints().isEmpty());
    // not expected to change
    assertEquals(Datasets.DATASET_LANGUAGE, dataset.getLanguage());
    assertEquals(Datasets.DATASET_RIGHTS, dataset.getRights());
    // per https://github.com/gbif/registry/issues/4, Citation is now generated
    assertNotNull(dataset.getCitation());
    assertEquals(
        Datasets.buildExpectedCitation(dataset, Organizations.ORGANIZATION_TITLE),
        dataset.getCitation().getText());
    assertEquals(Datasets.DATASET_ABBREVIATION, dataset.getAbbreviation());
    assertEquals(Datasets.DATASET_ALIAS, dataset.getAlias());
  }

  /**
   * Retrieve persisted IPT installation, and make a series of assertions to ensure it has been
   * properly persisted.
   *
   * @param installationKey installation key (UUID)
   * @param organizationKey installation hosting organization key
   * @return validated installation
   */
  private Installation validatePersistedIptInstallation(
      UUID installationKey, UUID organizationKey) {
    // retrieve installation anew
    Installation installation = installationService.get(installationKey);

    assertNotNull(installation, "Installation should be present");
    assertEquals(organizationKey, installation.getOrganizationKey());
    assertEquals(InstallationType.IPT_INSTALLATION, installation.getType());
    assertEquals(IPT_NAME, installation.getTitle());
    assertEquals(IPT_DESCRIPTION, installation.getDescription());
    assertNotNull(installation.getCreated());
    assertNotNull(installation.getModified());

    // check installation's primary contact was properly persisted
    Contact contact = installation.getContacts().get(0);
    assertNotNull(contact, "Installation primary contact should be present");
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
    assertNotNull(endpoint, "Installation FEED endpoint should be present");
    assertNotNull(endpoint.getKey());
    assertEquals(IPT_SERVICE_URL, endpoint.getUrl());
    assertEquals(EndpointType.FEED, endpoint.getType());
    assertNotNull(endpoint.getCreated());
    assertNotNull(endpoint.getCreatedBy());
    assertNotNull(endpoint.getModified());
    assertNotNull(endpoint.getModifiedBy());

    return installation;
  }

  /**
   * Retrieve persisted IPT dataset, and make a series of assertions to ensure it has been properly
   * persisted.
   *
   * @param datasetKey installation key (UUID)
   * @param organizationKey installation publishing organization key
   * @return validated installation
   */
  private Dataset validatePersistedIptDataset(
      UUID datasetKey, UUID organizationKey, UUID installationKey, DatasetType datasetType) {
    // retrieve installation anew
    Dataset dataset = datasetService.get(datasetKey);

    assertNotNull(dataset, "Dataset should be present");
    assertEquals(organizationKey, dataset.getPublishingOrganizationKey());
    assertEquals(installationKey, dataset.getInstallationKey());
    assertEquals(datasetType, dataset.getType());
    assertEquals(Requests.DATASET_NAME, dataset.getTitle());
    assertEquals(Requests.DATASET_DESCRIPTION, dataset.getDescription());
    assertNotNull(dataset.getCreated());
    assertNotNull(dataset.getModified());

    // check dataset's primary contact was properly persisted
    Contact contact = dataset.getContacts().get(0);
    assertNotNull(contact, "Dataset primary contact should be present");
    assertNotNull(contact.getKey());
    assertTrue(contact.isPrimary());
    assertEquals(Requests.DATASET_PRIMARY_CONTACT_NAME, contact.getFirstName());
    assertEquals(Requests.DATASET_PRIMARY_CONTACT_EMAIL, contact.getEmail());
    assertEquals(Requests.DATASET_PRIMARY_CONTACT_PHONE, contact.getPhone());
    assertEquals(Requests.DATASET_PRIMARY_CONTACT_ADDRESS, contact.getAddress());
    assertEquals(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT, contact.getType());
    assertNotNull(contact.getCreated());
    assertNotNull(contact.getCreatedBy());
    assertNotNull(contact.getModified());
    assertNotNull(contact.getModifiedBy());

    // check dataset's EML & DWC_ARCHIVE endpoints were properly persisted
    Endpoint endpoint = dataset.getEndpoints().get(0);
    assertNotNull(endpoint, "Dataset ARCHIVE endpoint should be present");
    assertNotNull(endpoint.getKey());
    assertEquals(DATASET_OCCURRENCE_SERVICE_URL, endpoint.getUrl());
    assertTrue(
        endpoint.getType().equals(EndpointType.DWC_ARCHIVE)
            || endpoint.getType().equals(EndpointType.EML));
    assertNotNull(endpoint.getCreated());
    assertNotNull(endpoint.getCreatedBy());
    assertNotNull(endpoint.getModified());
    assertNotNull(endpoint.getModifiedBy());

    endpoint = dataset.getEndpoints().get(1);
    assertNotNull(endpoint, "Dataset EML endpoint should be present");
    assertNotNull(endpoint.getKey());
    assertEquals(DATASET_EML_SERVICE_URL, endpoint.getUrl());
    assertTrue(
        endpoint.getType().equals(EndpointType.DWC_ARCHIVE)
            || endpoint.getType().equals(EndpointType.EML));
    assertNotNull(endpoint.getCreated());
    assertNotNull(endpoint.getCreatedBy());
    assertNotNull(endpoint.getModified());
    assertNotNull(endpoint.getModifiedBy());

    return dataset;
  }

  /**
   * Populate a list of name value pairs used in the common ws requests for IPT registrations and
   * updates. </br> Basically a copy of the method in the IPT, to ensure the parameter names are
   * identical.
   *
   * @param organizationKey organization key (UUID)
   * @return list of name value pairs, or an empty list if the IPT or organisation key were null
   */
  private MultiValueMap<String, String> buildIptParameters(UUID organizationKey) {
    MultiValueMap<String, String> data = new LinkedMultiValueMap<>();

    // main
    data.add(ORGANIZATION_KEY_PARAM, organizationKey.toString());
    data.add(NAME_PARAM, IPT_NAME);
    data.add(DESCRIPTION_PARAM, IPT_DESCRIPTION);

    // primary contact
    data.add(PRIMARY_CONTACT_TYPE_PARAM, IPT_PRIMARY_CONTACT_TYPE);
    data.add(PRIMARY_CONTACT_NAME_PARAM, IPT_PRIMARY_CONTACT_NAME);
    data.add(PRIMARY_CONTACT_EMAIL_PARAM, IPT_PRIMARY_CONTACT_EMAIL.get(0));

    // service/endpoint
    data.add(SERVICE_TYPES_PARAM, IPT_SERVICE_TYPE);
    data.add(SERVICE_URLS_PARAM, IPT_SERVICE_URL.toASCIIString());

    // add IPT password used for updating the IPT's own metadata & issuing atomic updateURL
    // operations
    data.add(WS_PASSWORD_PARAM, IPT_WS_PASSWORD);

    return data;
  }

  /**
   * Populate a list of name value pairs used in the common ws requests for IPT dataset
   * registrations and updates. </br> Basically a copy of the method in the IPT, to ensure the
   * parameter names are identical.
   *
   * @param installationKey installation key
   * @return list of name value pairs, or an empty list if the dataset or organisation key were null
   */
  private MultiValueMap<String, String> buildIptDatasetParameters(UUID installationKey) {
    MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
    // main
    data.add(NAME_PARAM, Requests.DATASET_NAME);
    data.add(DESCRIPTION_PARAM, Requests.DATASET_DESCRIPTION);
    data.add(HOMEPAGE_URL_PARAM, Requests.DATASET_HOMEPAGE_URL);
    data.add(LOGO_URL_PARAM, Requests.DATASET_LOGO_URL);

    // primary contact
    data.add(PRIMARY_CONTACT_TYPE_PARAM, Requests.DATASET_PRIMARY_CONTACT_TYPE);
    data.add(PRIMARY_CONTACT_EMAIL_PARAM, Requests.DATASET_PRIMARY_CONTACT_EMAIL.get(0));
    data.add(PRIMARY_CONTACT_NAME_PARAM, Requests.DATASET_PRIMARY_CONTACT_NAME);
    data.add(PRIMARY_CONTACT_ADDRESS_PARAM, Requests.DATASET_PRIMARY_CONTACT_ADDRESS.get(0));
    data.add(PRIMARY_CONTACT_PHONE_PARAM, Requests.DATASET_PRIMARY_CONTACT_PHONE.get(0));

    // endpoint(s)
    data.add(SERVICE_TYPES_PARAM, DATASET_SERVICE_TYPES);
    data.add(SERVICE_URLS_PARAM, DATASET_SERVICE_URLS);

    // add additional ipt and organisation parameters
    data.add(IPT_KEY_PARAM, installationKey.toString());

    return data;
  }
}
