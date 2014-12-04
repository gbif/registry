package org.gbif.registry.ws.resources.legacy;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.utils.Contacts;
import org.gbif.registry.utils.Datasets;
import org.gbif.registry.utils.Endpoints;
import org.gbif.registry.utils.Installations;
import org.gbif.registry.utils.Organizations;
import org.gbif.registry.utils.Parsers;
import org.gbif.registry.utils.Requests;
import org.gbif.registry.ws.util.LegacyResourceConstants;
import org.gbif.utils.HttpUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;

import com.google.common.base.Charsets;
import com.google.inject.Injector;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.SAXException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LegacyDatasetResourceIT {

  // Flushes the database on each run
  @ClassRule
  public static final LiquibaseInitializer liquibaseRule = new LiquibaseInitializer(RegistryTestModules.database());

  @ClassRule
  public static final RegistryServer registryServer = RegistryServer.INSTANCE;

  @Rule
  public final DatabaseInitializer databaseRule = new DatabaseInitializer(RegistryTestModules.database());

  private final DatasetService datasetService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public LegacyDatasetResourceIT() throws ParserConfigurationException, SAXException {
    Injector i = RegistryTestModules.webservice();
    this.datasetService = i.getInstance(DatasetService.class);
  }

  /**
   * The test begins by persisting a new Organization, and installation associated to that Organization.
   * </br>
   * Then, it sends a register Dataset (POST) request to create a new Dataset owned by this organization. The request
   * does not contain an installation key, but it can be inferred that it should use the key of the one and only
   * installation belonging to the publishing organization. The request also doesn't create any endpoints, that's done in
   * a separate legacy ws call. Therefore, the DatasetType defaults to METADATA.
   * </br>
   * Upon receiving an HTTP Response, the test parses its XML content in order to extract the registered Dataset UUID
   * for example. The content is parsed exactly the same way as the GBRDS WS consumer would do it.
   * </br>
   * Last, the test validates that the dataset was persisted correctly.
   */
  @Test
  public void testRegisterLegacyDataset() throws IOException, URISyntaxException, SAXException {
    // persist new organization (Dataset publishing organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // persist new installation of type IPT
    Installation installation = Installations.newPersistedInstance(organizationKey);
    UUID installationKey = installation.getKey();

    // populate params for ws, without installation key
    List<NameValuePair> data = buildLegacyDatasetParameters(organizationKey);
    UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(data, Charset.forName("UTF-8"));

    // construct request uri
    String uri = Requests.getRequestUri("/registry/resource");

    // send POST request with credentials
    HttpUtil.Response result = Requests.http.post(uri, null, null, Organizations.credentials(organization), uefe);

    // parse newly registered IPT key (UUID)
    Parsers.saxParser.parse(Parsers.getUtf8Stream(result.content), Parsers.legacyDatasetResponseHandler);
    assertNotNull("Registered Dataset key should be in response",
      UUID.fromString(Parsers.legacyDatasetResponseHandler.key));

    // some information that should have been updated
    Dataset dataset =
      validatePersistedLegacyDataset(UUID.fromString(Parsers.legacyDatasetResponseHandler.key), organizationKey,
        installationKey);

    // some additional information to check
    assertNotNull(dataset.getCreatedBy());
    assertNotNull(dataset.getModifiedBy());
  }

  /**
   * The test begins by persisting a new Organization, Installation associated to the Organization, and Dataset
   * associated to the Organization. A primary contact and endpoint is then added to the Dataset.
   * </br>
   * Then, it sends an update Dataset (POST) request to update the same Dataset. The request does not have the primary
   * contact, endpoint, or installation key form parameters. Since the organization only has 1 installation anyways,
   * it will be inferred that the dataset belongs to this one.
   * </br>
   * Upon receiving an HTTP Response, the test parses its XML content in order to extract the registered Dataset UUID
   * for example. It also ensures that the primary contact and endpoints still exist.
   */
  @Test
  public void testUpdateLegacyDatasetWithNoContactNoEndpointNoInstallationKey()
    throws IOException, URISyntaxException, SAXException {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // persist new installation of type IPT
    Installation installation = Installations.newPersistedInstance(organizationKey);
    UUID installationKey = installation.getKey();

    // persist new Dataset associated to installation
    Dataset dataset = Datasets.newPersistedInstance(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();
    // add primary contact to Dataset
    Contact c = Contacts.newInstance();
    c.setType(ContactType.TECHNICAL_POINT_OF_CONTACT);
    datasetService.addContact(datasetKey, c);
    // add endpoint to Dataset
    Endpoint e = Endpoints.newInstance();
    datasetService.addEndpoint(datasetKey, e);

    // validate it
    validateExistingDataset(dataset, organizationKey, installationKey);

    // before sending the update POST request, count the number of datasets, contacts and endpoints
    assertEquals(1, datasetService.list(new PagingRequest(0, 10)).getResults().size());
    assertEquals(1, datasetService.listEndpoints(datasetKey).size());
    assertEquals(1, datasetService.listContacts(datasetKey).size());

    // some information never going to change
    Date created = dataset.getCreated();
    assertNotNull(created);
    String createdBy = dataset.getCreatedBy();
    assertNotNull(createdBy);

    // populate params for ws
    List<NameValuePair> data = new ArrayList<NameValuePair>();
    // main
    data.add(new BasicNameValuePair(LegacyResourceConstants.NAME_PARAM, Requests.DATASET_NAME));
    data.add(new BasicNameValuePair(LegacyResourceConstants.NAME_LANGUAGE_PARAM, Requests.DATASET_NAME_LANGUAGE));
    data.add(new BasicNameValuePair(LegacyResourceConstants.DESCRIPTION_PARAM, Requests.DATASET_DESCRIPTION));
    data.add(new BasicNameValuePair(LegacyResourceConstants.DOI_PARAM, Requests.DOI));
    data.add(new BasicNameValuePair(LegacyResourceConstants.DESCRIPTION_LANGUAGE_PARAM,
      Requests.DATASET_DESCRIPTION_LANGUAGE));
    data.add(new BasicNameValuePair(LegacyResourceConstants.HOMEPAGE_URL_PARAM, Requests.DATASET_HOMEPAGE_URL));
    data.add(new BasicNameValuePair(LegacyResourceConstants.LOGO_URL_PARAM, Requests.DATASET_LOGO_URL));
    // add additional ipt and organisation parameters
    data.add(new BasicNameValuePair(LegacyResourceConstants.ORGANIZATION_KEY_PARAM, organizationKey.toString()));

    UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(data, Charset.forName("UTF-8"));

    // construct request uri
    String uri = Requests.getRequestUri("/registry/resource/" + datasetKey.toString());

    // send POST request with credentials
    HttpUtil.Response result = Requests.http.post(uri, null, null, Organizations.credentials(organization), uefe);

    // if Jersey was responding with default UTF-8 (fine in Intellij, not fine via maven) conversion below not needed
    String st = new String(result.content.getBytes(), Charsets.UTF_8);
    // parse updated registered Dataset key (UUID)

    Parsers.saxParser.parse(Parsers.getUtf8Stream(st), Parsers.legacyDatasetResponseHandler);

    assertNotNull("Updated Dataset key should be in response", Parsers.legacyDatasetResponseHandler.key);
    assertEquals(datasetKey.toString(), Parsers.legacyDatasetResponseHandler.key);
    assertNotNull("Updated Dataset organizationKey should be in response",
      Parsers.legacyDatasetResponseHandler.organisationKey);
    assertEquals(organizationKey.toString(), Parsers.legacyDatasetResponseHandler.organisationKey);

    // make some additional assertions that the update was successful
    // retrieve installation anew
    dataset = datasetService.get(datasetKey);

    assertNotNull("Dataset should be present", dataset);
    assertEquals(organizationKey, dataset.getPublishingOrganizationKey());
    assertEquals(installationKey, dataset.getInstallationKey());
    assertEquals(DatasetType.OCCURRENCE, dataset.getType());
    assertEquals(Requests.DATASET_NAME, dataset.getTitle());
    assertEquals(Requests.DATASET_NAME_LANGUAGE, dataset.getLanguage().getIso2LetterCode());
    assertEquals(Requests.DATASET_DESCRIPTION, dataset.getDescription());
    assertEquals(Requests.DATASET_HOMEPAGE_URL, dataset.getHomepage().toString());
    assertEquals(Requests.DATASET_LOGO_URL, dataset.getLogoUrl().toString());
    assertNotNull(dataset.getCreated());
    assertEquals(created.toString(), dataset.getCreated().toString());
    assertEquals(createdBy, dataset.getCreatedBy());
    assertNotNull(dataset.getModified());
  }

  /**
   * The test sends a get all datasets owned by organization (GET) request, the JSON response having at the very least
   * the dataset key, publishing organization key, dataset title, and dataset description.
   */
  @Test
  public void testGetLegacyDatasetsForOrganizationJSON() throws IOException, URISyntaxException, SAXException {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // persist new installation of type IPT
    Installation installation = Installations.newPersistedInstance(organizationKey);
    UUID installationKey = installation.getKey();

    // persist new Dataset associated to installation
    Dataset dataset = Datasets.newPersistedInstance(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();

    // construct request uri
    String uri = Requests.getRequestUri("/registry/resource.json?organisationKey=" + organizationKey.toString());

    // send GET request with no credentials
    HttpUtil.Response result = Requests.http.get(uri);

    // JSON array expected, with single resource
    assertTrue(result.content.startsWith("[") && result.content.endsWith("]"));
    JsonNode rootNode = objectMapper.readTree(result.content);
    assertEquals(1, rootNode.size());
    // keys "key" and "name" expected
    assertEquals(datasetKey.toString(), rootNode.get(0).get("key").getTextValue());
    assertEquals(dataset.getPublishingOrganizationKey().toString(), rootNode.get(0).get("organisationKey").getTextValue());
    assertEquals(dataset.getTitle(), rootNode.get(0).get("name").getTextValue());
  }

  /**
   * The test sends a get all datasets owned by organization (GET) request, the XML response having at the very least
   * the dataset key, publishing organization key, dataset title, and dataset description.
   */
  @Test
  public void testGetLegacyDatasetsForOrganizationXML() throws IOException, URISyntaxException, SAXException {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // persist new installation of type IPT
    Installation installation = Installations.newPersistedInstance(organizationKey);
    UUID installationKey = installation.getKey();

    // persist new Dataset associated to installation
    Dataset dataset = Datasets.newPersistedInstance(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();

    // construct request uri
    String uri = Requests.getRequestUri("/registry/resource?organisationKey=" + organizationKey.toString());

    // send GET request with no credentials
    HttpUtil.Response result = Requests.http.get(uri);

    // TODO: Response must be wrapped with root <resources>, not <legacyDatasetResponses>
    assertTrue(result.content.contains("<legacyDatasetResponses><resource>"));
    // verify character encoding here already, known to cause issue on some systems
    assertTrue(result.content.contains("Türkei"));

    // parse newly registered list of datasets
    Parsers.saxParser.parse(Parsers.getUtf8Stream(result.content), Parsers.legacyDatasetResponseHandler);
    assertEquals(datasetKey.toString(), Parsers.legacyDatasetResponseHandler.key);
    assertEquals(organizationKey.toString(), Parsers.legacyDatasetResponseHandler.organisationKey);
    assertEquals(dataset.getTitle(), Parsers.legacyDatasetResponseHandler.name);
  }

  /**
   * The test sends a get dataset (GET) request, the JSON response having all of: key, organisationKey, name,
   * description, nameLanguage, descriptionLanguage, homepageURL, primaryContactType/Name/Email/Phone/Address/Desc.
   */
  @Test
  public void testGetLegacyDatasetJSON() throws IOException, URISyntaxException, SAXException {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // persist new installation of type IPT
    Installation installation = Installations.newPersistedInstance(organizationKey);
    UUID installationKey = installation.getKey();

    // persist new Dataset associated to installation
    Dataset dataset = Datasets.newPersistedInstance(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();

    // add primary contact to Dataset
    Contact c = Contacts.newInstance();
    c.setType(ContactType.TECHNICAL_POINT_OF_CONTACT);
    datasetService.addContact(dataset.getKey(), c);

    // construct request uri
    String uri = Requests.getRequestUri("/registry/resource/" + datasetKey.toString() + ".json");

    // send GET request with no credentials
    HttpUtil.Response result = Requests.http.get(uri);

    // JSON object expected, representing single dataset
    assertTrue(result.content.startsWith("{") && result.content.endsWith("}"));
    JsonNode rootNode = objectMapper.readTree(result.content);
    // keys "key" and "name" expected
    assertEquals(datasetKey.toString(), rootNode.get("key").getTextValue());
    assertEquals(dataset.getPublishingOrganizationKey().toString(), rootNode.get("organisationKey").getTextValue());
    assertEquals(dataset.getTitle(), rootNode.get("name").getTextValue());
    assertEquals(dataset.getDescription(), rootNode.get("description").getTextValue());
    assertEquals(dataset.getLanguage().getIso2LetterCode(), rootNode.get("nameLanguage").getTextValue());
    assertEquals(dataset.getLanguage().getIso2LetterCode(), rootNode.get("descriptionLanguage").getTextValue());
    assertEquals(dataset.getHomepage().toString(), rootNode.get("homepageURL").getTextValue());
    assertEquals(LegacyResourceConstants.TECHNICAL_CONTACT_TYPE, rootNode.get("primaryContactType").getTextValue());
    assertEquals("Tim Robertson", rootNode.get("primaryContactName").getTextValue());
    assertEquals("+45 28261487", rootNode.get("primaryContactPhone").getTextValue());
    assertEquals("trobertson@gbif.org", rootNode.get("primaryContactEmail").getTextValue());
    assertEquals("Universitetsparken 15", rootNode.get("primaryContactAddress").getTextValue());
    assertEquals("About 175cm, geeky, scruffy...", rootNode.get("primaryContactDescription").getTextValue());
  }

  /**
   * The test sends a get dataset (GET) request for a dataset that does not exist.
   * The JSON response having an error message, not a 404.
   */
  @Test
  public void testGetLegacyDatasetNotFoundJSON() throws IOException, URISyntaxException, SAXException {
    // construct request uri
    String uri = Requests.getRequestUri("/registry/resource/" + UUID.randomUUID().toString() + ".json");

    // send GET request with no credentials
    HttpUtil.Response result = Requests.http.get(uri);

    // JSON object expected, representing single dataset
    assertTrue(result.content.equalsIgnoreCase("{\"error\":\"No resource matches the key provided\"}"));
  }

  /**
   * The test sends a get dataset (GET) request, the XML response having all of: key, organisationKey, name,
   * description, nameLanguage, descriptionLanguage, homepageURL, primaryContactType/Name/Email/Phone/Address/Desc.
   */
  @Test
  public void testGetLegacyDatasetXML() throws IOException, URISyntaxException, SAXException {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // persist new installation of type IPT
    Installation installation = Installations.newPersistedInstance(organizationKey);
    UUID installationKey = installation.getKey();

    // persist new Dataset associated to installation
    Dataset dataset = Datasets.newPersistedInstance(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();

    // add primary contact to Dataset
    Contact c = Contacts.newInstance();
    c.setType(ContactType.TECHNICAL_POINT_OF_CONTACT);
    datasetService.addContact(dataset.getKey(), c);

    // construct request uri
    String uri = Requests.getRequestUri("/registry/resource/" + datasetKey.toString());

    // send GET request with no credentials
    HttpUtil.Response result = Requests.http.get(uri);

    // XML expected, parse Dataset
    assertTrue(result.content.contains("<resource>"));
    Parsers.saxParser.parse(Parsers.getUtf8Stream(result.content), Parsers.legacyDatasetResponseHandler);

    assertEquals(dataset.getKey().toString(), Parsers.legacyDatasetResponseHandler.key);
    assertEquals(dataset.getTitle(), Parsers.legacyDatasetResponseHandler.name);
    assertEquals(dataset.getLanguage().getIso2LetterCode(), Parsers.legacyDatasetResponseHandler.nameLanguage);
    assertEquals(dataset.getDescription(), Parsers.legacyDatasetResponseHandler.description);
    assertEquals(dataset.getLanguage().getIso2LetterCode(), Parsers.legacyDatasetResponseHandler.descriptionLanguage);
    assertEquals(dataset.getHomepage().toString(), Parsers.legacyDatasetResponseHandler.homepageURL);
    assertEquals(LegacyResourceConstants.TECHNICAL_CONTACT_TYPE,
      Parsers.legacyDatasetResponseHandler.primaryContactType);
    assertEquals("Tim Robertson", Parsers.legacyDatasetResponseHandler.primaryContactName);
    assertEquals("trobertson@gbif.org", Parsers.legacyDatasetResponseHandler.primaryContactEmail);
    assertEquals("Universitetsparken 15", Parsers.legacyDatasetResponseHandler.primaryContactAddress);
    assertEquals("+45 28261487", Parsers.legacyDatasetResponseHandler.primaryContactPhone);
    assertEquals("About 175cm, geeky, scruffy...", Parsers.legacyDatasetResponseHandler.primaryContactDescription);
  }

  /**
   * The test sends a get all datasets owned by organization (GET) request, the JSON response having at the very least
   * the dataset key, publishing organization key, dataset title, and dataset description.
   */
  @Test
  public void testGetLegacyDatasetsForOrganizationThatDoesNotExist()
    throws IOException, URISyntaxException, SAXException {
    // construct request uri using Organization that doesn't exist
    String uri = Requests.getRequestUri("/registry/resource.json?organisationKey=" + UUID.randomUUID().toString());

    // send GET request with no credentials
    HttpUtil.Response result = Requests.http.get(uri);

    JsonNode rootNode = objectMapper.readTree(result.content);

    // JSON array expected, with single entry
    assertEquals(1, rootNode.size());
    String error = rootNode.toString();
    assertEquals("{\"error\":\"No organisation matches the key provided\"}", error);
  }

  /**
   * Populate a list of name value pairs used in the common ws requests for GBRDS dataset registrations and updates.
   *
   * @param organizationKey organization key
   * @return list of name value pairs
   */
  private List<NameValuePair> buildLegacyDatasetParameters(UUID organizationKey) {
    List<NameValuePair> data = new ArrayList<NameValuePair>();
    // main
    data.add(new BasicNameValuePair(LegacyResourceConstants.NAME_PARAM, Requests.DATASET_NAME));
    data.add(new BasicNameValuePair(LegacyResourceConstants.DOI_PARAM, Requests.DOI));
    data.add(new BasicNameValuePair(LegacyResourceConstants.NAME_LANGUAGE_PARAM, Requests.DATASET_NAME_LANGUAGE));
    data.add(new BasicNameValuePair(LegacyResourceConstants.DESCRIPTION_PARAM, Requests.DATASET_DESCRIPTION));
    data.add(new BasicNameValuePair(LegacyResourceConstants.DESCRIPTION_LANGUAGE_PARAM,
      Requests.DATASET_DESCRIPTION_LANGUAGE));
    data.add(new BasicNameValuePair(LegacyResourceConstants.HOMEPAGE_URL_PARAM, Requests.DATASET_HOMEPAGE_URL));
    data.add(new BasicNameValuePair(LegacyResourceConstants.LOGO_URL_PARAM, Requests.DATASET_LOGO_URL));

    // primary contact
    data.add(new BasicNameValuePair(LegacyResourceConstants.PRIMARY_CONTACT_TYPE_PARAM,
      Requests.DATASET_PRIMARY_CONTACT_TYPE));
    data.add(new BasicNameValuePair(LegacyResourceConstants.PRIMARY_CONTACT_EMAIL_PARAM,
      Requests.DATASET_PRIMARY_CONTACT_EMAIL.get(0)));
    data.add(new BasicNameValuePair(LegacyResourceConstants.PRIMARY_CONTACT_NAME_PARAM,
      Requests.DATASET_PRIMARY_CONTACT_NAME));
    data.add(new BasicNameValuePair(LegacyResourceConstants.PRIMARY_CONTACT_ADDRESS_PARAM,
      Requests.DATASET_PRIMARY_CONTACT_ADDRESS.get(0)));
    data.add(new BasicNameValuePair(LegacyResourceConstants.PRIMARY_CONTACT_PHONE_PARAM,
      Requests.DATASET_PRIMARY_CONTACT_PHONE.get(0)));
    data.add(new BasicNameValuePair(LegacyResourceConstants.PRIMARY_CONTACT_DESCRIPTION_PARAM,
      Requests.DATASET_PRIMARY_CONTACT_DESCRIPTION));

    // add additional ipt and organisation parameters
    data.add(new BasicNameValuePair(LegacyResourceConstants.ORGANIZATION_KEY_PARAM, organizationKey.toString()));
    return data;
  }

  /**
   * Retrieve persisted Legacy (GBRDS) dataset, and make a series of assertions to ensure it has been properly
   * persisted.
   *
   * @param datasetKey installation key (UUID)
   * @param organizationKey installation publishing organization key
   * @return validated installation
   */
  private Dataset validatePersistedLegacyDataset(UUID datasetKey, UUID organizationKey, UUID installationKey) {
    // retrieve installation anew
    Dataset dataset = datasetService.get(datasetKey);


    assertNotNull("Dataset should be present", dataset);
    assertEquals(organizationKey, dataset.getPublishingOrganizationKey());
    assertEquals(installationKey, dataset.getInstallationKey());
    assertEquals(DatasetType.METADATA, dataset.getType());
    assertEquals(Requests.DATASET_NAME, dataset.getTitle());
    assertEquals(new DOI(Requests.DOI), dataset.getDoi()); // ensure that we handle the parsing
    assertEquals(Requests.DATASET_NAME_LANGUAGE, dataset.getLanguage().getIso2LetterCode());
    assertEquals(Requests.DATASET_DESCRIPTION, dataset.getDescription());
    assertEquals(Requests.DATASET_HOMEPAGE_URL, dataset.getHomepage().toString());
    assertEquals(Requests.DATASET_LOGO_URL, dataset.getLogoUrl().toString());
    assertNotNull(dataset.getCreated());
    assertNotNull(dataset.getModified());

    // check dataset's primary contact was properly persisted
    Contact contact = dataset.getContacts().get(0);
    assertNotNull("Dataset primary contact should be present", contact);
    assertNotNull(contact.getKey());
    assertTrue(contact.isPrimary());
    assertEquals(Requests.DATASET_PRIMARY_CONTACT_NAME, contact.getFirstName());
    assertEquals(Requests.DATASET_PRIMARY_CONTACT_EMAIL, contact.getEmail());
    assertEquals(Requests.DATASET_PRIMARY_CONTACT_PHONE, contact.getPhone());
    assertEquals(Requests.DATASET_PRIMARY_CONTACT_ADDRESS, contact.getAddress());
    assertEquals(Requests.DATASET_PRIMARY_CONTACT_DESCRIPTION, contact.getDescription());
    assertEquals(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT, contact.getType());
    assertNotNull(contact.getCreated());
    assertNotNull(contact.getCreatedBy());
    assertNotNull(contact.getModified());
    assertNotNull(contact.getModifiedBy());

    return dataset;
  }

  /**
   * Retrieve dataset presumed already to exist, and make a series of assertions to ensure it is valid.
   *
   * @param dataset dataset
   * @param organizationKey publishing organization key
   * @param installationKey installation key
   */
  private void validateExistingDataset(Dataset dataset, UUID organizationKey, UUID installationKey) {
    assertNotNull("Dataset should be present", dataset);
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
    assertEquals(Datasets.DATASET_CITATION.getIdentifier(), dataset.getCitation().getIdentifier());
    assertEquals(Datasets.DATASET_ABBREVIATION, dataset.getAbbreviation());
    assertEquals(Datasets.DATASET_ALIAS, dataset.getAlias());
  }
}
