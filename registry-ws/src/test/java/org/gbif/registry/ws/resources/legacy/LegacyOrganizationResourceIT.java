package org.gbif.registry.ws.resources.legacy;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.utils.Contacts;
import org.gbif.registry.utils.Organizations;
import org.gbif.registry.utils.Parsers;
import org.gbif.registry.utils.Requests;
import org.gbif.registry.ws.resources.OrganizationResource;
import org.gbif.registry.ws.util.LegacyResourceConstants;
import org.gbif.utils.HttpUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import javax.xml.parsers.ParserConfigurationException;

import com.google.inject.Injector;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.SAXException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LegacyOrganizationResourceIT {

  // Flushes the database on each run
  @ClassRule
  public static final LiquibaseInitializer liquibaseRule = new LiquibaseInitializer(RegistryTestModules.database());

  @ClassRule
  public static final RegistryServer registryServer = RegistryServer.INSTANCE;

  @Rule
  public final DatabaseInitializer databaseRule = new DatabaseInitializer(RegistryTestModules.database());

  private final OrganizationService organizationService;
  private final NodeService nodeService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public LegacyOrganizationResourceIT() throws ParserConfigurationException, SAXException {
    Injector i = RegistryTestModules.webservice();
    this.organizationService = i.getInstance(OrganizationResource.class);
    this.nodeService = i.getInstance(NodeService.class);
  }

  /**
   * The test sends a get Organization (GET) request with no parameters and .json, signifying that the response must be
   * JSON.
   */
  @Test
  public void testGetOrganizationJSON() throws IOException, URISyntaxException, SAXException {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();
    Contact c = Contacts.newInstance();
    c.setType(ContactType.TECHNICAL_POINT_OF_CONTACT);
    organizationService.addContact(organization.getKey(), c);

    // construct request uri
    String uri = Requests.getRequestUri("/registry/organisation/" + organization.getKey().toString() + ".json");

    // send GET request with no credentials
    HttpUtil.Response result = Requests.http.get(uri);

    // JSON expected
    assertTrue(result.content.startsWith("{"));
    JsonNode rootNode = objectMapper.readTree(result.content);

    // JSON object expected
    assertEquals(15, rootNode.size());

    assertEquals(organization.getKey().toString(), rootNode.get("key").getTextValue());
    assertEquals(organization.getTitle(), rootNode.get("name").getTextValue());
    assertEquals(organization.getLanguage().getIso2LetterCode(), rootNode.get("nameLanguage").getTextValue());
    assertEquals(organization.getDescription(), rootNode.get("description").getTextValue());
    assertEquals(organization.getLanguage().getIso2LetterCode(), rootNode.get("descriptionLanguage").getTextValue());
    assertEquals(organization.getHomepage().toString(), rootNode.get("homepageURL").getTextValue());
    assertEquals(LegacyResourceConstants.TECHNICAL_CONTACT_TYPE, rootNode.get("primaryContactType").getTextValue());
    assertEquals("Tim Robertson", rootNode.get("primaryContactName").getTextValue());
    assertEquals("trobertson@gbif.org", rootNode.get("primaryContactEmail").getTextValue());
    assertEquals("Universitetsparken 15", rootNode.get("primaryContactAddress").getTextValue());
    assertEquals("+45 28261487", rootNode.get("primaryContactPhone").getTextValue());
    assertEquals(organization.getEndorsingNodeKey().toString(), rootNode.get("nodeKey").getTextValue());
    assertEquals("The UK National Node", rootNode.get("nodeName").getTextValue());
    assertEquals("", rootNode.get("nodeContactEmail").getTextValue());
  }

  /**
   * The test sends a get Organization (GET) request with no parameters and .json, signifying that the response must be
   * JSON.
   */
  @Test
  public void testGetOrganizationXML() throws IOException, URISyntaxException, SAXException {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();
    Contact c = Contacts.newInstance();
    c.setType(ContactType.TECHNICAL_POINT_OF_CONTACT);
    organizationService.addContact(organization.getKey(), c);

    // construct request uri
    String uri = Requests.getRequestUri("/registry/organisation/" + organization.getKey().toString());

    // send GET request with no credentials
    HttpUtil.Response result = Requests.http.get(uri);

    // XML expected, parse Organization
    Parsers.saxParser.parse(Parsers.getUtf8Stream(result.content), Parsers.legacyOrganizationResponseHandler);

    assertEquals(organization.getKey().toString(), Parsers.legacyOrganizationResponseHandler.key);
    assertEquals(organization.getTitle(), Parsers.legacyOrganizationResponseHandler.name);
    assertEquals(organization.getLanguage().getIso2LetterCode(), Parsers.legacyOrganizationResponseHandler.nameLanguage);
    assertEquals(organization.getDescription(), Parsers.legacyOrganizationResponseHandler.description);
    assertEquals(organization.getLanguage().getIso2LetterCode(), Parsers.legacyOrganizationResponseHandler.descriptionLanguage);
    assertEquals(organization.getHomepage().toString(), Parsers.legacyOrganizationResponseHandler.homepageURL);
    assertEquals(LegacyResourceConstants.TECHNICAL_CONTACT_TYPE, Parsers.legacyOrganizationResponseHandler.primaryContactType);
    assertEquals("Tim Robertson", Parsers.legacyOrganizationResponseHandler.primaryContactName);
    assertEquals("trobertson@gbif.org", Parsers.legacyOrganizationResponseHandler.primaryContactEmail);
    assertEquals("Universitetsparken 15", Parsers.legacyOrganizationResponseHandler.primaryContactAddress);
    assertEquals("+45 28261487", Parsers.legacyOrganizationResponseHandler.primaryContactPhone);

    Node node = nodeService.get(organization.getEndorsingNodeKey());
    assertEquals(node.getTitle(), Parsers.legacyOrganizationResponseHandler.nodeName);
    assertEquals("", Parsers.legacyOrganizationResponseHandler.nodeContactEmail);
    assertEquals(organization.getEndorsingNodeKey().toString(), Parsers.legacyOrganizationResponseHandler.nodeKey);
  }

  /**
   * The test sends a get Organization (GET) request with callback=? parameter, signifying that the response must be
   * JSONP.
   */
  @Test
  public void testGetOrganizationCallback() throws IOException, URISyntaxException, SAXException {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();

    // construct request uri
    String uri = Requests.getRequestUri("/registry/organisation/" + organization.getKey().toString()
                                        + ".json?callback=jQuery15106997501577716321_1384974875868&_=1384974903371");

    // send GET request with no credentials
    HttpUtil.Response result = Requests.http.get(uri);

    // JSONP expected
    assertTrue(result.content.startsWith("jQuery15106997501577716321_1384974875868({"));

  }

  /**
   * The test sends a get Organization (GET) request with op=login parameter, basically a test to check if
   * the organization credentials (key/password) supplied are correct.
   */
  @Test
  public void testGetOrganizationLogin() throws IOException, URISyntaxException, SAXException {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();

    // construct request uri
    String uri =
      Requests.getRequestUri("/registry/organisation/" + organization.getKey().toString() + ".json?op=login");

    // send GET request with credentials, but no form encoded parameters
    HttpUtil.Response result = Requests.http.get(uri, null, Organizations.credentials(organization));

    // OK 201 response expected
    assertEquals(200, result.getStatusCode());
  }

  /**
   * The test sends a get all organizations (GET) request, the JSON response having a key and name for each
   * organisation in the list.
   */
  @Test
  public void testGetOrganizationsJSON() throws IOException, URISyntaxException, SAXException {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();

    // construct request uri
    String uri = Requests.getRequestUri("/registry/organisation.json");

    // send GET request with no credentials
    HttpUtil.Response result = Requests.http.get(uri);

    // JSON array expected, with single entry
    assertTrue(result.content.startsWith("[") && result.content.endsWith("]"));
    JsonNode rootNode = objectMapper.readTree(result.content);
    assertEquals(1, rootNode.size());
    // keys "key" and "name" expected
    assertEquals(organization.getKey().toString(), rootNode.get(0).get("key").getTextValue());
    assertEquals(organization.getTitle(), rootNode.get(0).get("name").getTextValue());
  }

  /**
   * The test sends a get all organizations (GET) request, the XML response having a key and name for each
   * organisation in the list.
   */
  @Test
  public void testGetOrganizationsXML() throws IOException, URISyntaxException, SAXException {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();

    // construct request uri
    String uri = Requests.getRequestUri("/registry/organisation");

    // send GET request with no credentials
    HttpUtil.Response result = Requests.http.get(uri);

    // TODO: Response should be wrapped with root <organisations>, not <legacyOrganizationBriefResponses>
    assertTrue(result.content.contains("<legacyOrganizationBriefResponses><organisation>"));

    // parse newly registered list of datasets
    Parsers.saxParser.parse(Parsers.getUtf8Stream(result.content), Parsers.legacyOrganizationResponseHandler);
    assertEquals(organization.getKey().toString(), Parsers.legacyOrganizationResponseHandler.key);
    assertEquals(organization.getTitle(), Parsers.legacyOrganizationResponseHandler.name);
  }

  /**
   * The test sends a password reminder (GET) request with op=password parameter, which triggers an email to the
   * primary contact of the organization with the password included.
   */
  @Test
  public void testGetOrganizationPasswordReminder() throws IOException, URISyntaxException, SAXException {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();
    Contact c = Contacts.newInstance();
    c.setType(ContactType.TECHNICAL_POINT_OF_CONTACT);
    organizationService.addContact(organization.getKey(), c);

    // construct request uri
    String uri =
      Requests.getRequestUri("/registry/organisation/" + organization.getKey().toString() + ".json?op=password");

    // send GET request with no credentials
    HttpUtil.Response result = Requests.http.get(uri);

    // OK 201 response expected
    assertEquals(200, result.getStatusCode());
    assertEquals(
      "<html><body><b>The password reminder was sent successfully to the email: </b>trobertson@gbif.org</body></html>",
      result.content);
  }

  /**
   * The test sends a password reminder (GET) request with op=password parameter, using an organization whose primary
   * contact doesn't have an email address. A Internal Server Error 500 response is expected.
   */
  @Test
  public void testGetOrganizationPasswordReminderServerError() throws IOException, URISyntaxException, SAXException {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();
    Contact c = Contacts.newInstance();
    c.setType(ContactType.TECHNICAL_POINT_OF_CONTACT);
    // override email, set to null
    c.setEmail(null);
    organizationService.addContact(organization.getKey(), c);

    // construct request uri
    String uri =
      Requests.getRequestUri("/registry/organisation/" + organization.getKey().toString() + ".json?op=password");

    // send GET request with no credentials
    HttpUtil.Response result = Requests.http.get(uri);

    // 500 Internal Server Error expected
    assertEquals(500, result.getStatusCode());
  }
}
