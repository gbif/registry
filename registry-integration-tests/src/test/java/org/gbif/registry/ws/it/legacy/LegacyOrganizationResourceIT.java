/*
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
package org.gbif.registry.ws.it.legacy;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.domain.ws.LegacyOrganizationBriefResponse;
import org.gbif.registry.domain.ws.LegacyOrganizationBriefResponseListWrapper;
import org.gbif.registry.domain.ws.LegacyOrganizationResponse;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.registry.ws.it.fixtures.RequestTestFixture;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.TECHNICAL_CONTACT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class LegacyOrganizationResourceIT extends BaseItTest {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer(database.getPostgresContainer());

  private final OrganizationService organizationService;
  private final NodeService nodeService;
  private final TestDataFactory testDataFactory;
  private final RequestTestFixture requestTestFixture;

  @Autowired
  public LegacyOrganizationResourceIT(
      OrganizationService organizationService,
      NodeService nodeService,
      TestDataFactory testDataFactory,
      RequestTestFixture requestTestFixture,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer) {
    super(principalProvider, esServer);
    this.organizationService = organizationService;
    this.nodeService = nodeService;
    this.testDataFactory = testDataFactory;
    this.requestTestFixture = requestTestFixture;
  }

  /**
   * The test sends a get Organization (GET) request with no parameters and .json, signifying that
   * the response must be JSON.
   */
  @Test
  public void testGetOrganizationJSON() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    Contact c = testDataFactory.newContact();
    c.setType(ContactType.TECHNICAL_POINT_OF_CONTACT);
    organizationService.addContact(organization.getKey(), c);

    // construct request uri
    String uri = "/registry/organisation/" + organization.getKey() + ".json";

    // send GET request with no credentials
    ResultActions actions =
        requestTestFixture.getRequest(uri).andExpect(status().is2xxSuccessful());

    // JSON expected
    LegacyOrganizationResponse response =
        requestTestFixture.extractJsonResponse(actions, LegacyOrganizationResponse.class);

    assertNotNull(organization.getKey());
    assertEquals(organization.getKey().toString(), response.getKey());
    assertEquals(organization.getTitle(), response.getName());
    assertEquals(organization.getLanguage().getIso2LetterCode(), response.getNameLanguage());
    assertEquals(organization.getDescription(), response.getDescription());
    assertEquals(organization.getLanguage().getIso2LetterCode(), response.getDescriptionLanguage());
    assertNotNull(organization.getHomepage());
    assertEquals(organization.getHomepage().toString(), response.getHomepageURL());
    assertEquals(TECHNICAL_CONTACT_TYPE, response.getPrimaryContactType());
    assertEquals("Tim Robertson", response.getPrimaryContactName());
    assertEquals("trobertson@gbif.org", response.getPrimaryContactEmail());
    assertEquals("Universitetsparken 15", response.getPrimaryContactAddress());
    assertEquals("+45 28261487", response.getPrimaryContactPhone());
    assertEquals(organization.getEndorsingNodeKey().toString(), response.getNodeKey());
    assertEquals("The UK National Node", response.getNodeName());
    assertEquals("", response.getNodeContactEmail());
  }

  /**
   * The test sends a get Organization (GET) request with no parameters and .json, signifying that
   * the response must be JSON.
   */
  @Test
  public void testGetOrganizationXML() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    Contact c = testDataFactory.newContact();
    c.setType(ContactType.TECHNICAL_POINT_OF_CONTACT);
    organizationService.addContact(organization.getKey(), c);

    // construct request uri
    String uri = "/registry/organisation/" + organization.getKey();

    // send GET request with no credentials
    ResultActions actions =
        requestTestFixture.getRequest(uri).andExpect(status().is2xxSuccessful());

    // XML expected, parse Organization
    LegacyOrganizationResponse response =
        requestTestFixture.extractXmlResponse(actions, LegacyOrganizationResponse.class);

    assertNotNull(organization.getKey());
    assertEquals(organization.getKey().toString(), response.getKey());
    assertEquals(organization.getTitle(), response.getName());
    assertEquals(organization.getLanguage().getIso2LetterCode(), response.getNameLanguage());
    assertEquals(organization.getDescription(), response.getDescription());
    assertEquals(organization.getLanguage().getIso2LetterCode(), response.getDescriptionLanguage());
    assertNotNull(organization.getHomepage());
    assertEquals(organization.getHomepage().toString(), response.getHomepageURL());
    assertEquals(TECHNICAL_CONTACT_TYPE, response.getPrimaryContactType());
    assertEquals("Tim Robertson", response.getPrimaryContactName());
    assertEquals("trobertson@gbif.org", response.getPrimaryContactEmail());
    assertEquals("Universitetsparken 15", response.getPrimaryContactAddress());
    assertEquals("+45 28261487", response.getPrimaryContactPhone());

    Node node = nodeService.get(organization.getEndorsingNodeKey());
    assertEquals(node.getTitle(), response.getNodeName());
    assertEquals("", response.getNodeContactEmail());
    assertEquals(organization.getEndorsingNodeKey().toString(), response.getNodeKey());
  }

  /**
   * The test sends a get Organization (GET) request with callback=? parameter, signifying that the
   * response must be JSONP.
   */
  @Test
  public void testGetOrganizationCallback() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();

    // construct request uri
    String uri =
        "/registry/organisation/"
            + organization.getKey()
            + ".json?callback=jQuery15106997501577716321_1384974875868&_=1384974903371";

    // send GET request with no credentials
    ResultActions actions =
        requestTestFixture.getRequest(uri).andExpect(status().is2xxSuccessful());

    String content = requestTestFixture.extractResponse(actions);

    // JSONP expected
    assertTrue(content.startsWith("jQuery15106997501577716321_1384974875868({"));
  }

  /**
   * The test sends a get Organization (GET) request with op=login parameter, basically a test to
   * check if the organization credentials (key/password) supplied are correct.
   */
  @Test
  public void testGetOrganizationLogin() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    assertNotNull(organization.getKey());

    // construct request uri
    String uri = "/registry/organisation/" + organization.getKey() + ".json?op=login";

    // send GET request with credentials, but no form encoded parameters
    // OK 200 response expected
    requestTestFixture
        .getRequest(organization.getKey().toString(), organization.getPassword(), uri)
        .andExpect(status().isOk());
  }

  /**
   * The test sends a get all organizations (GET) request, the JSON response having a key and name
   * for each organisation in the list.
   */
  @Test
  public void testGetOrganizationsJSON() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    assertNotNull(organization.getKey());

    // construct request uri
    String uri = "/registry/organisation.json";

    // send GET request with no credentials
    ResultActions actions =
        requestTestFixture.getRequest(uri).andExpect(status().is2xxSuccessful());

    // JSON array expected, with single entry
    LegacyOrganizationBriefResponseListWrapper responseWrapper =
        requestTestFixture.extractJsonResponse(
            actions, LegacyOrganizationBriefResponseListWrapper.class);

    LegacyOrganizationBriefResponse response =
        responseWrapper.getLegacyOrganizationBriefResponses().get(0);

    assertEquals(1, responseWrapper.getLegacyOrganizationBriefResponses().size());
    // keys "key" and "name" expected
    assertEquals(organization.getKey().toString(), response.getKey());
    assertEquals(organization.getTitle(), response.getName());
  }

  /**
   * The test sends a get all organizations (GET) request, the XML response having a key and name
   * for each organisation in the list.
   */
  @Test
  public void testGetOrganizationsXML() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    assertNotNull(organization.getKey());

    // construct request uri
    String uri = "/registry/organisation";

    // send GET request with no credentials
    ResultActions actions =
        requestTestFixture.getRequest(uri).andExpect(status().is2xxSuccessful());

    // parse newly registered list of datasets
    LegacyOrganizationBriefResponseListWrapper responseWrapper =
        requestTestFixture.extractXmlResponse(
            actions, LegacyOrganizationBriefResponseListWrapper.class);
    LegacyOrganizationBriefResponse response =
        responseWrapper.getLegacyOrganizationBriefResponses().get(0);
    assertEquals(organization.getKey().toString(), response.getKey());
    assertEquals(organization.getTitle(), response.getName());
  }

  /**
   * The test sends a password reminder (GET) request with op=password parameter, which triggers an
   * email to the primary contact of the organization with the password included.
   */
  @Test
  public void testGetOrganizationPasswordReminder() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    Contact c = testDataFactory.newContact();
    c.setType(ContactType.TECHNICAL_POINT_OF_CONTACT);
    organizationService.addContact(organization.getKey(), c);

    // construct request uri
    String uri = "/registry/organisation/" + organization.getKey() + ".json?op=password";

    // send GET request with no credentials
    ResultActions actions = requestTestFixture.getRequest(uri).andExpect(status().isOk());

    String content = requestTestFixture.extractResponse(actions);
    assertEquals(
        "<html><body><b>The password reminder was sent successfully to the email: </b>trobertson@gbif.org</body></html>",
        content);
  }

  /**
   * The test sends a password reminder (GET) request with op=password parameter, using an
   * organization whose primary contact doesn't have an email address. A Internal Server Error 500
   * response is expected.
   */
  @Test
  public void testGetOrganizationPasswordReminderServerError() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    assertNotNull(organization.getKey());
    Contact c = testDataFactory.newContact();
    c.setType(ContactType.TECHNICAL_POINT_OF_CONTACT);
    // override email, set to null
    c.setEmail(null);
    organizationService.addContact(organization.getKey(), c);

    // construct request uri
    String uri = "/registry/organisation/" + organization.getKey() + ".json?op=password";

    // send GET request with no credentials
    // 500 Internal Server Error expected
    requestTestFixture.getRequest(uri).andExpect(status().isInternalServerError());
  }
}
