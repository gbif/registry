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
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.utils.Datasets;
import org.gbif.registry.utils.Installations;
import org.gbif.registry.utils.Organizations;
import org.gbif.registry.utils.Parsers;
import org.gbif.registry.utils.Requests;
import org.gbif.registry.ws.util.LegacyResourceConstants;
import org.gbif.utils.HttpUtil;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;

import com.google.inject.Injector;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.SAXException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IptResourceIT {

  // Flushes the database on each run
  @ClassRule
  public static final LiquibaseInitializer liquibaseRule = new LiquibaseInitializer(RegistryTestModules.database());

  @ClassRule
  public static final RegistryServer registryServer = RegistryServer.INSTANCE;

  @Rule
  public final DatabaseInitializer databaseRule = new DatabaseInitializer(RegistryTestModules.database());

  private final InstallationService installationService;
  private final DatasetService datasetService;

  // set of HTTP form parameters sent in POST request
  private static final String IPT_NAME = "Test IPT Registry2";
  private static final String IPT_DESCRIPTION = "Description of Test IPT";
  private static final String IPT_PRIMARY_CONTACT_TYPE = "technical";
  private static final String IPT_PRIMARY_CONTACT_NAME = "Kyle Braak";
  private static final String IPT_PRIMARY_CONTACT_EMAIL = "kbraak@gbif.org";
  private static final String IPT_SERVICE_TYPE = "RSS";
  private static final URI IPT_SERVICE_URL = URI.create("http://ipt.gbif.org/rss.do");
  private static final String IPT_WS_PASSWORD = "password";

  private static final String DATASET_SERVICE_TYPES = "EML|DWC-ARCHIVE-OCCURRENCE";
  private static final String DATASET_SERVICE_URLS =
    "http://ipt.gbif.org/eml.do?r=ds123|http://ipt.gbif.org/archive.do?r=ds123";
  private static final URI DATASET_EML_SERVICE_URL = URI.create("http://ipt.gbif.org/eml.do?r=ds123");
  private static final URI DATASET_OCCURRENCE_SERVICE_URL = URI.create("http://ipt.gbif.org/archive.do?r=ds123");

  public IptResourceIT() throws ParserConfigurationException, SAXException {
    Injector i = RegistryTestModules.webservice();
    this.installationService = i.getInstance(InstallationService.class);
    this.datasetService = i.getInstance(DatasetService.class);
  }

  /**
   * The test begins by persisting a new Organization.
   * </br>
   * Then, it sends a register IPT (POST) request to create a new Installation associated to this organization.
   * The request is issued against the web services running on the local Grizzly test server. The request is sent in
   * exactly the same way as the IPT would send it, using the URL path (/ipt/register), URL encoded form parameters,
   * and basic authentication. The web service authorizes the request, and then persists the Installation, associated
   * to the Organization.
   * </br>
   * Upon receiving an HTTP Response, the test parses its XML content in order to extract the registered IPT UUID for
   * example. The content is parsed exactly the same way as the IPT would do it.
   * </br>
   * Last, the test validates that the installation was persisted correctly.
   */
  @Test
  public void testRegisterIpt() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // populate params for ws
    List<NameValuePair> data = buildIPTParameters(organizationKey);

    UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(data, Charset.forName("UTF-8"));

    // construct request uri
    String uri = Requests.getRequestUri("/registry/ipt/register");

    // send POST request with credentials
    HttpUtil.Response result = Requests.http.post(uri, null, null, Organizations.credentials(organization), uefe);

    // correct response code?
    assertEquals(Response.Status.CREATED.getStatusCode(), result.getStatusCode());

    // parse newly registered IPT key (UUID)
    Parsers.saxParser.parse(Parsers.getStream(result.content), Parsers.legacyIptEntityHandler);
    assertNotNull("Registered IPT key should be in response", UUID.fromString(Parsers.legacyIptEntityHandler.key));

    // some information that should have been updated
    Installation installation =
      validatePersistedIptInstallation(UUID.fromString(Parsers.legacyIptEntityHandler.key), organizationKey);

    // some additional information to check
    assertNotNull(installation.getCreatedBy());
    assertNotNull(installation.getModifiedBy());
  }

  /**
   * The test begins by persisting a new Organization, and Installation associated to the Organization.
   * </br>
   * Then, it sends an update IPT (POST) request to update the same Installation. The request is issued against the
   * web services running on the local Grizzly test server. The request is sent in exactly the same way as the IPT
   * would send it, using the URL path (/ipt/update/{key}), URL encoded form parameters, and basic authentication. The
   * web service authorizes the request, and then persists the Installation, updating its information.
   * </br>
   * Upon receiving an HTTP Response, the test parses its XML content in order to extract the registered IPT UUID for
   * example. The content is parsed exactly the same way as the IPT would do it.
   * </br>
   * Next, the test validates that the Installation's information was updated correctly. The same request is then
   * resent once more, and the test validates that no duplicate installation, contact, or endpoint was created.
   */
  @Test
  public void testUpdateIpt() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // persist new installation of type IPT
    Installation installation = Installations.newPersistedInstance(organizationKey);
    UUID installationKey = installation.getKey();

    // validate it
    validateExistingIptInstallation(installation, organizationKey);

    // some information never going to change
    Date created = installation.getCreated();
    assertNotNull(created);
    String createdBy = installation.getCreatedBy();
    assertNotNull(createdBy);

    // populate params for ws
    List<NameValuePair> data = buildIPTParameters(organizationKey);

    UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(data, Charset.forName("UTF-8"));

    // construct request uri
    String uri = Requests.getRequestUri("/registry/ipt/update/" + installationKey.toString());

    // send POST request with credentials
    HttpUtil.Response result = Requests.http.post(uri, null, null, Installations.credentials(installation), uefe);

    // correct response code? Jersey resource should really respond with 201, but 2XX means success
    assertEquals(Response.Status.CREATED.getStatusCode(), result.getStatusCode());

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

    // send same POST request again, to check that duplicate contact and endpoints don't get persisted
    Requests.http.post(uri, null, null, Installations.credentials(installation), uefe);

    // retrieve newly updated installation, and make sure the same number of installations, contacts and endpoints exist
    assertEquals(1, installationService.list(new PagingRequest(0, 10)).getResults().size());
    installation = validatePersistedIptInstallation(installationKey, organizationKey);
    assertEquals(1, installation.getContacts().size());
    assertEquals(1, installation.getEndpoints().size());

    // compare contact key and make sure it doesn't change after update (Contacts are mutable)
    assertEquals(String.valueOf(contactKey), String.valueOf(installation.getContacts().get(0).getKey()));
    // compare endpoint key and make sure it does change after update (Endpoints are not mutable)
    assertNotEquals(String.valueOf(endpointKey), String.valueOf(installation.getEndpoints().get(0).getKey()));
  }

  /**
   * The test sends a update IPT (POST) request to create a new IPT, however, its organizationKey HTTP
   * Parameter doesn't match the organization key used in the credentials. The test must check that the server responds
   * with a 401 Unauthorized Response.
   */
  @Test
  public void testUpdateIptButNotAuthorized() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // persist new installation of type IPT
    Installation installation = Installations.newPersistedInstance(organizationKey);
    UUID installationKey = installation.getKey();

    // populate params for ws
    List<NameValuePair> data = buildIPTParameters(organizationKey);

    UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(data, Charset.forName("UTF-8"));

    // construct request uri
    String uri = Requests.getRequestUri("/registry/ipt/update/" + installationKey.toString());

    // send POST request with WRONG credentials
    // assign the installation the random generated key, to provoke authorization failure
    installation.setKey(UUID.randomUUID());
    // send POST request with credentials
    HttpUtil.Response result = Requests.http.post(uri, null, null, Installations.credentials(installation), uefe);

    // 401 expected
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), result.getStatusCode());
  }

  /**
   * The test sends an update IPT (POST) request to update an Installation, however, it is missing a mandatory HTTP
   * Parameter for the primary contact email. The test must check that the server responds with a 400 BAD_REQUEST
   * Response.
   */
  @Test
  public void testUpdateIptWithNoPrimaryContact() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // persist new installation of type IPT
    Installation installation = Installations.newPersistedInstance(organizationKey);
    UUID installationKey = installation.getKey();

    // populate params for ws
    List<NameValuePair> data = buildIPTParameters(organizationKey);

    assertEquals(9, data.size());
    // remove mandatory key/value before sending
    Iterator<NameValuePair> iter = data.iterator();
    while (iter.hasNext()) {
      NameValuePair pair = iter.next();
      if (pair.getName().equals("primaryContactEmail")) {
        iter.remove();
      }
    }
    assertEquals(8, data.size());

    UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(data, Charset.forName("UTF-8"));

    // construct request uri
    String uri = Requests.getRequestUri("/registry/ipt/update/" + installationKey.toString());

    // send POST request with credentials
    HttpUtil.Response result = Requests.http.post(uri, null, null, Installations.credentials(installation), uefe);

    // 400 expected
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), result.getStatusCode());
  }

  /**
   * The test sends a register IPT (POST) request to create a new Installation, however, its organizationKey HTTP
   * Parameter doesn't match the organization key used in the credentials. The test must check that the server responds
   * with a 401 Unauthorized Response.
   */
  @Test
  public void testRegisterIptButNotAuthorized() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();

    // populate params for ws
    List<NameValuePair> data = buildIPTParameters(organization.getKey());

    UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(data, Charset.forName("UTF-8"));

    // construct request uri
    String uri = Requests.getRequestUri("/registry/ipt/register");

    // send POST request with WRONG credentials
    // assign the organization the random generated key, to provoke authorization failure
    organization.setKey(UUID.randomUUID());
    HttpUtil.Response result = Requests.http.post(uri, null, null, Organizations.credentials(organization), uefe);

    // 401 expected
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), result.getStatusCode());
  }

  /**
   * The test sends a register IPT (POST) request to create a new Installation, however, it is missing a mandatory HTTP
   * Parameter for the primary contact email. The test must check that the server responds with a 400 BAD_REQUEST
   * Response.
   */
  @Test
  public void testRegisterIptWithNoPrimaryContact() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();

    // populate params for ws
    List<NameValuePair> data = buildIPTParameters(organization.getKey());

    assertEquals(9, data.size());
    // remove mandatory key/value before sending
    Iterator<NameValuePair> iter = data.iterator();
    while (iter.hasNext()) {
      NameValuePair pair = iter.next();
      if (pair.getName().equals("primaryContactEmail")) {
        iter.remove();
      }
    }
    assertEquals(8, data.size());

    UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(data, Charset.forName("UTF-8"));

    // construct request uri
    String uri = Requests.getRequestUri("/registry/ipt/register");

    // send POST request with credentials so that it passes authorization
    HttpUtil.Response result = Requests.http.post(uri, null, null, Organizations.credentials(organization), uefe);

    // 400 expected
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), result.getStatusCode());
  }

  /**
   * The test begins by persisting a new Organization and IPT Installation.
   * </br>
   * Then, it sends a register Dataset (POST) request to create a new Dataset owned by this organization and associated
   * to this IPT installation. The request is issued against the web services running on the local Grizzly test server.
   * The request is sent in exactly the same way as the IPT would send it, using the URL path (/ipt/resource), URL
   * encoded form parameters, and basic authentication. The web service authorizes the request, and then persists the
   * Installation, associated to the Organization/Installation.
   * </br>
   * Upon receiving an HTTP Response, the test parses its XML content in order to extract the registered Dataset UUID
   * for example. The content is parsed exactly the same way as the IPT would do it.
   * </br>
   * Last, the test validates that the dataset was persisted correctly.
   */
  @Test
  public void testRegisterIptDataset() throws Exception {
    // persist new organization (Dataset owning organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // persist new installation of type IPT
    Installation installation = Installations.newPersistedInstance(organizationKey);
    UUID installationKey = installation.getKey();

    // populate params for ws
    List<NameValuePair> data = buildIptDatasetParameters(installationKey);
    // organisationKey param included on register, not on update
    data.add(new BasicNameValuePair(LegacyResourceConstants.ORGANIZATION_KEY_PARAM, organizationKey.toString()));

    UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(data, Charset.forName("UTF-8"));

    // construct request uri
    String uri = Requests.getRequestUri("/registry/ipt/resource");

    // send POST request with credentials
    HttpUtil.Response result = Requests.http.post(uri, null, null, Organizations.credentials(organization), uefe);

    // correct response code?
    assertEquals(Response.Status.CREATED.getStatusCode(), result.getStatusCode());

    // parse newly registered IPT key (UUID)
    Parsers.saxParser.parse(Parsers.getStream(result.content), Parsers.legacyIptEntityHandler);
    assertNotNull("Registered Dataset key should be in response", UUID.fromString(Parsers.legacyIptEntityHandler.key));

    // some information that should have been updated
    Dataset dataset = validatePersistedIptDataset(UUID.fromString(Parsers.legacyIptEntityHandler.key), organizationKey,
      installationKey);

    // some additional information to check
    assertNotNull(dataset.getCreatedBy());
    assertNotNull(dataset.getModifiedBy());
  }

  /**
   * The test sends a register Dataset (POST) request to create a new Dataset, however, its organizationKey HTTP
   * Parameter doesn't match the organization key used in the credentials. The test must check that the server responds
   * with a 401 Unauthorized Response.
   */
  @Test
  public void testRegisterIptDatasetButNotAuthorized() throws Exception {
    // persist new organization (Dataset owning organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // persist new installation of type IPT
    Installation installation = Installations.newPersistedInstance(organizationKey);
    UUID installationKey = installation.getKey();

    // populate params for ws
    List<NameValuePair> data = buildIptDatasetParameters(installationKey);
    // organisationKey param included on register, not on update
    data.add(new BasicNameValuePair(LegacyResourceConstants.ORGANIZATION_KEY_PARAM, organizationKey.toString()));

    UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(data, Charset.forName("UTF-8"));

    // construct request uri
    String uri = Requests.getRequestUri("/registry/ipt/resource");

    // send POST request with WRONG credentials
    // assign the organization the random generated key, to provoke authorization failure
    organization.setKey(UUID.randomUUID());
    HttpUtil.Response result = Requests.http.post(uri, null, null, Organizations.credentials(organization), uefe);

    // 401 expected
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), result.getStatusCode());
  }

  /**
   * The test sends a register Dataset (POST) request to create a new Dataset, however, it is missing a mandatory HTTP
   * Parameter for the primary contact email. The test must check that the server responds with a 400 BAD_REQUEST
   * Response.
   */
  @Test
  public void testRegisterIptDatasetWithNoPrimaryContact() throws Exception {
    // persist new organization (Dataset owning organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // persist new installation of type IPT
    Installation installation = Installations.newPersistedInstance(organizationKey);
    UUID installationKey = installation.getKey();

    // populate params for ws
    List<NameValuePair> data = buildIptDatasetParameters(installationKey);
    // organisationKey param included on register, not on update
    data.add(new BasicNameValuePair(LegacyResourceConstants.ORGANIZATION_KEY_PARAM, organizationKey.toString()));

    assertEquals(13, data.size());
    // remove mandatory key/value before sending
    Iterator<NameValuePair> iter = data.iterator();
    while (iter.hasNext()) {
      NameValuePair pair = iter.next();
      if (pair.getName().equals("primaryContactType")) {
        iter.remove();
      }
    }
    assertEquals(12, data.size());

    UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(data, Charset.forName("UTF-8"));

    // construct request uri
    String uri = Requests.getRequestUri("/registry/ipt/resource");

    // send POST request with credentials so that it passes authorization
    HttpUtil.Response result = Requests.http.post(uri, null, null, Organizations.credentials(organization), uefe);

    // 400 expected
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), result.getStatusCode());
  }

  /**
   * The test begins by persisting a new Organization, Installation associated to the Organization, and Dataset
   * associated to the Organization.
   * </br>
   * Then, it sends an update Dataset (POST) request to update the same Dataset. The request is issued against the
   * web services running on the local Grizzly test server. The request is sent in exactly the same way as the IPT
   * would send it, using the URL path (/ipt/resource/{key}), URL encoded form parameters, and basic authentication.
   * The
   * web service authorizes the request, and then persists the Dataset, updating its information.
   * </br>
   * Upon receiving an HTTP Response, the test parses its XML content in order to extract the registered Dataset UUID
   * for example. The content is parsed exactly the same way as the IPT would do it.
   * </br>
   * Next, the test validates that the Dataset's information was updated correctly. The same request is then
   * resent once more, and the test validates that no duplicate Dataset, contact, or endpoint was created.
   */
  @Test
  public void testUpdateIptDataset() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // persist new installation of type IPT
    Installation installation = Installations.newPersistedInstance(organizationKey);
    UUID installationKey = installation.getKey();

    // persist new Dataset associated to installation
    Dataset dataset = Datasets.newPersistedInstance(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();

    // validate it
    validateExistingIptDataset(dataset, organizationKey, installationKey);

    // some information never going to change
    Date created = dataset.getCreated();
    assertNotNull(created);
    String createdBy = dataset.getCreatedBy();
    assertNotNull(createdBy);

    // populate params for ws
    List<NameValuePair> data = buildIptDatasetParameters(installationKey);

    UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(data, Charset.forName("UTF-8"));

    // construct request uri
    String uri = Requests.getRequestUri("/registry/ipt/resource/" + datasetKey.toString());

    // send POST request with credentials
    HttpUtil.Response result = Requests.http.post(uri, null, null, Organizations.credentials(organization), uefe);

    // correct response code?
    assertEquals(Response.Status.CREATED.getStatusCode(), result.getStatusCode());

    // some information that should have been updated
    dataset = validatePersistedIptDataset(datasetKey, organizationKey, installationKey);

    // some additional information that should not have been updated
    assertEquals(created, dataset.getCreated());
    assertEquals(createdBy, dataset.getCreatedBy());
    assertEquals(Datasets.DATASET_LANGUAGE, dataset.getLanguage());
    assertEquals(Datasets.DATASET_RIGHTS, dataset.getRights());
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

    // send same POST request again, to check that duplicate contact and endpoints don't get persisted
    Requests.http.post(uri, null, null, Organizations.credentials(organization), uefe);

    // retrieve newly updated dataset, and make sure the same number of datasets, contacts and endpoints exist
    assertEquals(1, datasetService.list(new PagingRequest(0, 10)).getResults().size());
    dataset = validatePersistedIptDataset(datasetKey, organizationKey, installationKey);
    assertEquals(1, dataset.getContacts().size());
    assertEquals(2, dataset.getEndpoints().size());

    // compare contact key and make sure it doesn't change after update (Contacts are mutable)
    assertEquals(String.valueOf(contactKey), String.valueOf(dataset.getContacts().get(0).getKey()));
    // compare endpoint key and make sure it does change after update (Endpoints are not mutable)
    assertNotEquals(String.valueOf(endpointKey), String.valueOf(dataset.getEndpoints().get(0).getKey()));
  }

  /**
   * The test sends a update Dataset (POST) request to create a new Dataset, however, its organizationKey HTTP
   * Parameter doesn't match the organization key used in the credentials. The test must check that the server responds
   * with a 401 Unauthorized Response.
   */
  @Test
  public void testUpdateIptDatasetButNotAuthorized() throws Exception {
    // persist new organization (Dataset owning organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // persist new installation of type IPT
    Installation installation = Installations.newPersistedInstance(organizationKey);
    UUID installationKey = installation.getKey();

    // persist new Dataset associated to installation
    Dataset dataset = Datasets.newPersistedInstance(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();

    // populate params for ws
    List<NameValuePair> data = buildIptDatasetParameters(installationKey);

    UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(data, Charset.forName("UTF-8"));

    // construct request uri
    String uri = Requests.getRequestUri("/registry/ipt/resource/" + datasetKey.toString());

    // send POST request with WRONG credentials
    // assign the organization the random generated key, to provoke authorization failure
    organization.setKey(UUID.randomUUID());
    // send POST request with credentials
    HttpUtil.Response result = Requests.http.post(uri, null, null, Organizations.credentials(organization), uefe);

    // 401 expected
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), result.getStatusCode());
  }

  /**
   * The test sends an update Dataset (POST) request to update a Dataset, however, it is missing a mandatory HTTP
   * Parameter for the primary contact email. The test must check that the server responds with a 400 BAD_REQUEST
   * Response.
   */
  @Test
  public void testUpdateIptDatasetWithNoPrimaryContact() throws Exception {
    // persist new organization (Dataset owning organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // persist new installation of type IPT
    Installation installation = Installations.newPersistedInstance(organizationKey);
    UUID installationKey = installation.getKey();

    // persist new Dataset associated to installation
    Dataset dataset = Datasets.newPersistedInstance(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();

    // populate params for ws
    List<NameValuePair> data = buildIptDatasetParameters(installationKey);

    assertEquals(12, data.size());
    // remove mandatory key/value before sending
    Iterator<NameValuePair> iter = data.iterator();
    while (iter.hasNext()) {
      NameValuePair pair = iter.next();
      if (pair.getName().equals("primaryContactEmail")) {
        iter.remove();
      }
    }
    assertEquals(11, data.size());

    UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(data, Charset.forName("UTF-8"));

    // construct request uri
    String uri = Requests.getRequestUri("/registry/ipt/resource/" + datasetKey.toString());

    // send POST request with credentials
    HttpUtil.Response result = Requests.http.post(uri, null, null, Organizations.credentials(organization), uefe);

    // 400 expected
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), result.getStatusCode());
  }

  /**
   * The test begins by persisting a new Organization, Installation associated to the Organization, and Dataset
   * associated to the Organization.
   * </br>
   * Then, it sends an update Dataset (POST) request to update the same Dataset. This populates the primary contact
   * and endpoints.
   * </br>
   * Then, it sends a delete Dataset (POST) request to delete the Dataset.
   * </br>
   * Next, the test validates that the Dataset was deleted correctly.
   */
  @Test
  public void testDeleteIptDataset() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = Organizations.newPersistedInstance();
    UUID organizationKey = organization.getKey();

    // persist new installation of type IPT
    Installation installation = Installations.newPersistedInstance(organizationKey);
    UUID installationKey = installation.getKey();

    // persist new Dataset associated to installation
    Dataset dataset = Datasets.newPersistedInstance(organizationKey, installationKey);
    UUID datasetKey = dataset.getKey();

    // construct update request uri
    String uri = Requests.getRequestUri("/registry/ipt/resource/" + datasetKey.toString());

    // before sending the delete POST request, count the number of datasets, contacts and endpoints
    assertEquals(1, datasetService.list(new PagingRequest(0, 10)).getResults().size());

    // send delete POST request (using same URL)
    HttpUtil.Response result = Requests.http.delete(uri, Organizations.credentials(organization));

    // check that the dataset was deleted
    assertEquals(200, result.getStatusCode());
    assertEquals(0, datasetService.list(new PagingRequest(0, 10)).getResults().size());
  }

  /**
   * Retrieve installation presumed already to exist, and make a series of assertions to ensure it is valid.
   *
   * @param installation installation
   * @param organizationKey hosting organization key
   */
  private void validateExistingIptInstallation(Installation installation, UUID organizationKey) {
    assertNotNull("Installation should be present", installation);
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
   * Retrieve dataset presumed already to exist, and make a series of assertions to ensure it is valid.
   *
   * @param dataset dataset
   * @param organizationKey owning organization key
   * @param installationKey installation key
   */
  private void validateExistingIptDataset(Dataset dataset, UUID organizationKey, UUID installationKey) {
    assertNotNull("Dataset should be present", dataset);
    assertEquals(organizationKey, dataset.getOwningOrganizationKey());
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

  /**
   * Retrieve persisted IPT dataset, and make a series of assertions to ensure it has been properly persisted.
   *
   * @param datasetKey installation key (UUID)
   * @param organizationKey installation owning organization key
   * @return validated installation
   */
  private Dataset validatePersistedIptDataset(UUID datasetKey, UUID organizationKey, UUID installationKey) {
    // retrieve installation anew
    Dataset dataset = datasetService.get(datasetKey);

    assertNotNull("Dataset should be present", dataset);
    assertEquals(organizationKey, dataset.getOwningOrganizationKey());
    assertEquals(installationKey, dataset.getInstallationKey());
    assertEquals(DatasetType.OCCURRENCE, dataset.getType());
    assertEquals(Requests.DATASET_NAME, dataset.getTitle());
    assertEquals(Requests.DATASET_DESCRIPTION, dataset.getDescription());
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
    assertEquals(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT, contact.getType());
    assertNotNull(contact.getCreated());
    assertNotNull(contact.getCreatedBy());
    assertNotNull(contact.getModified());
    assertNotNull(contact.getModifiedBy());

    // check dataset's EML & DWC_ARCHIVE endpoints were properly persisted
    Endpoint endpoint = dataset.getEndpoints().get(0);
    assertNotNull("Dataset ARCHIVE endpoint should be present", endpoint);
    assertNotNull(endpoint.getKey());
    assertEquals(DATASET_OCCURRENCE_SERVICE_URL, endpoint.getUrl());
    assertTrue(endpoint.getType().equals(EndpointType.DWC_ARCHIVE) || endpoint.getType().equals(EndpointType.EML));
    assertNotNull(endpoint.getCreated());
    assertNotNull(endpoint.getCreatedBy());
    assertNotNull(endpoint.getModified());
    assertNotNull(endpoint.getModifiedBy());

    endpoint = dataset.getEndpoints().get(1);
    assertNotNull("Dataset EML endpoint should be present", endpoint);
    assertNotNull(endpoint.getKey());
    assertEquals(DATASET_EML_SERVICE_URL, endpoint.getUrl());
    assertTrue(endpoint.getType().equals(EndpointType.DWC_ARCHIVE) || endpoint.getType().equals(EndpointType.EML));
    assertNotNull(endpoint.getCreated());
    assertNotNull(endpoint.getCreatedBy());
    assertNotNull(endpoint.getModified());
    assertNotNull(endpoint.getModifiedBy());

    return dataset;
  }

  /**
   * Populate a list of name value pairs used in the common ws requests for IPT registrations and updates.
   * </br>
   * Basically a copy of the method in the IPT, to ensure the parameter names are identical.
   *
   * @param organizationKey organization key (UUID)
   * @return list of name value pairs, or an empty list if the IPT or organisation key were null
   */
  private List<NameValuePair> buildIPTParameters(UUID organizationKey) {
    List<NameValuePair> data = new ArrayList<NameValuePair>();
    // main
    data.add(new BasicNameValuePair(LegacyResourceConstants.ORGANIZATION_KEY_PARAM, organizationKey.toString()));
    data.add(new BasicNameValuePair(LegacyResourceConstants.NAME_PARAM, IPT_NAME));
    data.add(new BasicNameValuePair(LegacyResourceConstants.DESCRIPTION_PARAM, IPT_DESCRIPTION));

    // primary contact
    data.add(new BasicNameValuePair(LegacyResourceConstants.PRIMARY_CONTACT_TYPE_PARAM, IPT_PRIMARY_CONTACT_TYPE));
    data.add(new BasicNameValuePair(LegacyResourceConstants.PRIMARY_CONTACT_NAME_PARAM, IPT_PRIMARY_CONTACT_NAME));
    data.add(new BasicNameValuePair(LegacyResourceConstants.PRIMARY_CONTACT_EMAIL_PARAM, IPT_PRIMARY_CONTACT_EMAIL));

    // service/endpoint
    data.add(new BasicNameValuePair(LegacyResourceConstants.SERVICE_TYPES_PARAM, IPT_SERVICE_TYPE));
    data.add(new BasicNameValuePair(LegacyResourceConstants.SERVICE_URLS_PARAM, IPT_SERVICE_URL.toASCIIString()));

    // add IPT password used for updating the IPT's own metadata & issuing atomic updateURL operations
    data.add(new BasicNameValuePair(LegacyResourceConstants.WS_PASSWORD_PARAM, IPT_WS_PASSWORD));

    return data;
  }

  /**
   * Populate a list of name value pairs used in the common ws requests for IPT dataset registrations and updates.
   * </br>
   * Basically a copy of the method in the IPT, to ensure the parameter names are identical.
   *
   * @param installationKey installation key
   * @return list of name value pairs, or an empty list if the dataset or organisation key were null
   */
  private List<NameValuePair> buildIptDatasetParameters(UUID installationKey) {
    List<NameValuePair> data = new ArrayList<NameValuePair>();
    // main
    data.add(new BasicNameValuePair(LegacyResourceConstants.NAME_PARAM, Requests.DATASET_NAME));
    data.add(new BasicNameValuePair(LegacyResourceConstants.DESCRIPTION_PARAM, Requests.DATASET_DESCRIPTION));
    data.add(new BasicNameValuePair(LegacyResourceConstants.HOMEPAGE_URL_PARAM, Requests.DATASET_HOMEPAGE_URL));
    data.add(new BasicNameValuePair(LegacyResourceConstants.LOGO_URL_PARAM, Requests.DATASET_LOGO_URL));

    // primary contact
    data.add(new BasicNameValuePair(LegacyResourceConstants.PRIMARY_CONTACT_TYPE_PARAM,
      Requests.DATASET_PRIMARY_CONTACT_TYPE));
    data.add(new BasicNameValuePair(LegacyResourceConstants.PRIMARY_CONTACT_EMAIL_PARAM,
      Requests.DATASET_PRIMARY_CONTACT_EMAIL));
    data.add(new BasicNameValuePair(LegacyResourceConstants.PRIMARY_CONTACT_NAME_PARAM,
      Requests.DATASET_PRIMARY_CONTACT_NAME));
    data.add(new BasicNameValuePair(LegacyResourceConstants.PRIMARY_CONTACT_ADDRESS_PARAM,
      Requests.DATASET_PRIMARY_CONTACT_ADDRESS));
    data.add(new BasicNameValuePair(LegacyResourceConstants.PRIMARY_CONTACT_PHONE_PARAM,
      Requests.DATASET_PRIMARY_CONTACT_PHONE));

    // endpoint(s)
    data.add(new BasicNameValuePair(LegacyResourceConstants.SERVICE_TYPES_PARAM, DATASET_SERVICE_TYPES));
    data.add(new BasicNameValuePair(LegacyResourceConstants.SERVICE_URLS_PARAM, DATASET_SERVICE_URLS));

    // add additional ipt and organisation parameters
    data.add(new BasicNameValuePair(LegacyResourceConstants.IPT_KEY_PARAM, installationKey.toString()));

    return data;
  }
}
