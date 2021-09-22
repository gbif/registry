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
package org.gbif.registry.ws.it.legacy;

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
import org.gbif.api.vocabulary.License;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.domain.ws.ErrorResponse;
import org.gbif.registry.domain.ws.IptEntityResponse;
import org.gbif.registry.domain.ws.LegacyDatasetResponse;
import org.gbif.registry.domain.ws.LegacyDatasetResponseListWrapper;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.Datasets;
import org.gbif.registry.test.Organizations;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.registry.ws.it.fixtures.RequestTestFixture;
import org.gbif.registry.ws.it.fixtures.TestConstants;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.google.common.collect.Lists;

import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.DESCRIPTION_LANGUAGE_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.DESCRIPTION_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.DOI_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.HOMEPAGE_URL_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.LOGO_URL_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.NAME_LANGUAGE_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.NAME_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.ORGANIZATION_KEY_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.PRIMARY_CONTACT_ADDRESS_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.PRIMARY_CONTACT_DESCRIPTION_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.PRIMARY_CONTACT_EMAIL_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.PRIMARY_CONTACT_NAME_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.PRIMARY_CONTACT_PHONE_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.PRIMARY_CONTACT_TYPE_PARAM;
import static org.gbif.registry.domain.ws.util.LegacyResourceConstants.TECHNICAL_CONTACT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class LegacyDatasetResourceIT extends BaseItTest {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer();

  private final DatasetService datasetService;
  private final TestDataFactory testDataFactory;
  private final RequestTestFixture requestTestFixture;

  @Autowired
  public LegacyDatasetResourceIT(
      DatasetService datasetService,
      TestDataFactory testDataFactory,
      RequestTestFixture requestTestFixture,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer) {
    super(principalProvider, esServer);
    this.datasetService = datasetService;
    this.testDataFactory = testDataFactory;
    this.requestTestFixture = requestTestFixture;
  }

  /**
   * The test begins by persisting a new Organization, and installation associated to that
   * Organization. </br> Then, it sends a register Dataset (POST) request to create a new Dataset
   * owned by this organization. The request does not contain an installation key, but it can be
   * inferred that it should use the key of the one and only installation belonging to the
   * publishing organization. The request also doesn't create any endpoints, that's done in a
   * separate legacy ws call. Therefore, the DatasetType defaults to METADATA. </br> Upon receiving
   * an HTTP Response, the test parses its XML content in order to extract the registered Dataset
   * UUID for example. The content is parsed exactly the same way as the GBRDS WS consumer would do
   * it. </br> Last, the test validates that the dataset was persisted correctly.
   */
  @Test
  public void testRegisterLegacyDataset() throws Exception {
    // persist new organization (Dataset publishing organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    UUID organizationKey = organization.getKey();
    assertNotNull(organizationKey);

    // persist new installation of type IPT
    Installation installation = testDataFactory.newPersistedInstallation(organizationKey);
    UUID installationKey = installation.getKey();
    assertNotNull(installationKey);

    // populate params for ws, without installation key
    MultiValueMap<String, String> data = buildLegacyDatasetParameters(organizationKey);

    // construct request uri
    String uri = "/registry/resource";

    // send POST request with credentials
    ResultActions actions =
        requestTestFixture
            .postRequestUrlEncoded(data, organizationKey, organization.getPassword(), uri)
            .andExpect(status().is2xxSuccessful());

    // parse newly registered IPT key (UUID)
    IptEntityResponse iptEntityResponse =
        requestTestFixture.extractXmlResponse(actions, IptEntityResponse.class);

    assertNotNull(iptEntityResponse.getKey(), "Registered Dataset key should be in response");

    // some information that should have been updated
    Dataset dataset =
        validatePersistedLegacyDataset(
            UUID.fromString(iptEntityResponse.getKey()), organizationKey, installationKey);

    // some additional information to check
    assertNotNull(dataset.getCreatedBy());
    assertNotNull(dataset.getModifiedBy());
  }

  /**
   * The test begins by persisting a new Organization, Installation associated to the Organization,
   * and Dataset associated to the Organization. A primary contact and endpoint is then added to the
   * Dataset. </br> Then, it sends an update Dataset (POST) request to update the same Dataset. The
   * request does not have the primary contact, endpoint, or installation key form parameters. Since
   * the organization only has 1 installation anyways, it will be inferred that the dataset belongs
   * to this one. </br> Upon receiving an HTTP Response, the test parses its XML content in order to
   * extract the registered Dataset UUID for example. It also ensures that the primary contact and
   * endpoints still exist.
   */
  @Test
  public void testUpdateLegacyDatasetWithNoContactNoEndpointNoInstallationKey() throws Exception {
    // persist new organization (IPT hosting organization)
    Organization organization = testDataFactory.newPersistedOrganization();
    UUID organizationKey = organization.getKey();
    assertNotNull(organizationKey);

    // persist new installation of type IPT
    Installation installation = testDataFactory.newPersistedInstallation(organizationKey);
    UUID installationKey = installation.getKey();
    assertNotNull(installationKey);

    // persist new Dataset associated to installation, assigned CC-BY-NC 4.0
    Dataset dataset = testDataFactory.newPersistedDataset(organizationKey, installationKey);
    assertEquals(License.CC_BY_NC_4_0, dataset.getLicense());

    UUID datasetKey = dataset.getKey();
    assertNotNull(datasetKey);

    // add primary contact to Dataset
    Contact c = testDataFactory.newContact();
    c.setType(ContactType.TECHNICAL_POINT_OF_CONTACT);
    datasetService.addContact(datasetKey, c);
    // add endpoint to Dataset
    Endpoint e = testDataFactory.newEndpoint();
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
    MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
    // main
    data.add(NAME_PARAM, TestConstants.DATASET_NAME);
    data.add(NAME_LANGUAGE_PARAM, TestConstants.DATASET_NAME_LANGUAGE);
    data.add(DESCRIPTION_PARAM, TestConstants.DATASET_DESCRIPTION);
    data.add(DOI_PARAM, TestConstants.DOI);
    data.add(DESCRIPTION_LANGUAGE_PARAM, TestConstants.DATASET_DESCRIPTION_LANGUAGE);
    data.add(HOMEPAGE_URL_PARAM, TestConstants.DATASET_HOMEPAGE_URL);
    data.add(LOGO_URL_PARAM, TestConstants.DATASET_LOGO_URL);
    // add additional ipt and organisation parameters
    data.add(ORGANIZATION_KEY_PARAM, organizationKey.toString());

    // construct request uri
    String uri = "/registry/resource/" + datasetKey;

    // send POST request with credentials
    ResultActions actions =
        requestTestFixture
            .postRequestUrlEncoded(data, organizationKey, organization.getPassword(), uri)
            .andExpect(status().is2xxSuccessful());

    // parse updated registered Dataset key (UUID)
    LegacyDatasetResponse response =
        requestTestFixture.extractXmlResponse(actions, LegacyDatasetResponse.class);

    assertNotNull(response.getKey(), "Updated Dataset key should be in response");
    assertEquals(datasetKey.toString(), response.getKey());
    assertNotNull(
        response.getOrganisationKey(), "Updated Dataset organizationKey should be in response");
    assertEquals(organizationKey.toString(), response.getOrganisationKey());

    // make some additional assertions that the update was successful
    // retrieve installation anew
    dataset = datasetService.get(datasetKey);

    assertNotNull(dataset, "Dataset should be present");
    assertEquals(organizationKey, dataset.getPublishingOrganizationKey());
    assertEquals(installationKey, dataset.getInstallationKey());
    assertEquals(DatasetType.OCCURRENCE, dataset.getType());
    assertEquals(TestConstants.DATASET_NAME, dataset.getTitle());
    assertEquals(TestConstants.DATASET_NAME_LANGUAGE, dataset.getLanguage().getIso2LetterCode());
    assertEquals(TestConstants.DATASET_DESCRIPTION, dataset.getDescription());
    assertNotNull(dataset.getHomepage());
    assertEquals(TestConstants.DATASET_HOMEPAGE_URL, dataset.getHomepage().toString());
    assertNotNull(dataset.getLogoUrl());
    assertEquals(TestConstants.DATASET_LOGO_URL, dataset.getLogoUrl().toString());
    assertNotNull(dataset.getCreated());
    assertEquals(created.toString(), dataset.getCreated().toString());
    assertEquals(createdBy, dataset.getCreatedBy());
    assertNotNull(dataset.getModified());
  }

  /**
   * This test verifies http://dev.gbif.org/issues/browse/POR-2733 is solved. </br> The test begins
   * by persisting a new Organization, Installation associated to the Organization, and Dataset
   * associated to the Organization. An administrative primary contact with first name "Jan" and
   * last name "Legind" is then added to the Dataset. </br> Then, it sends an update Dataset (POST)
   * request to update the same Dataset. The request has the primary contact name "Jan Legind" that
   * used to try and override the first name "Jan". </br> Upon receiving an HTTP Response, the test
   * ensures the update was successful by parsing its XML content. Then it ensures that the primary
   * contact first name is still "Jan" not "Jan Legind".
   */
  @Test
  public void testUpdateLegacyDatasetWithExistingPrimaryContact() throws Exception {
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

    // add primary contact to Dataset
    Contact c = new Contact();
    c.setFirstName("Jan");
    c.setLastName("Legind");
    c.setEmail(Lists.newArrayList("jlegind@gbif.org"));
    c.setPrimary(true);
    c.setType(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT);
    datasetService.addContact(datasetKey, c);
    // add endpoint to Dataset
    Endpoint e = testDataFactory.newEndpoint();
    datasetService.addEndpoint(datasetKey, e);

    // validate it
    validateExistingDataset(dataset, organizationKey, installationKey);

    // before sending the update POST request, count the number of datasets, contacts and endpoints
    assertEquals(1, datasetService.list(new PagingRequest(0, 10)).getResults().size());
    assertEquals(1, datasetService.listEndpoints(datasetKey).size());
    assertEquals(1, datasetService.listContacts(datasetKey).size());

    Contact persisted = datasetService.listContacts(datasetKey).get(0);
    assertEquals("Jan", persisted.getFirstName());
    assertEquals("Legind", persisted.getLastName());
    assertEquals(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT, persisted.getType());

    // populate params for ws
    MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
    // main fields
    data.add(NAME_PARAM, TestConstants.DATASET_NAME);
    data.add(NAME_LANGUAGE_PARAM, TestConstants.DATASET_NAME_LANGUAGE);
    data.add(DESCRIPTION_PARAM, TestConstants.DATASET_DESCRIPTION);
    data.add(DOI_PARAM, TestConstants.DOI);
    data.add(DESCRIPTION_LANGUAGE_PARAM, TestConstants.DATASET_DESCRIPTION_LANGUAGE);
    data.add(HOMEPAGE_URL_PARAM, TestConstants.DATASET_HOMEPAGE_URL);
    data.add(LOGO_URL_PARAM, TestConstants.DATASET_LOGO_URL);
    // add additional ipt and organisation parameters
    data.add(ORGANIZATION_KEY_PARAM, organizationKey.toString());

    // primary contact with name "Jan Legind" and type "administrative"
    data.add(PRIMARY_CONTACT_TYPE_PARAM, TestConstants.DATASET_PRIMARY_CONTACT_TYPE);
    data.add(PRIMARY_CONTACT_EMAIL_PARAM, TestConstants.DATASET_PRIMARY_CONTACT_EMAIL.get(0));
    data.add(PRIMARY_CONTACT_NAME_PARAM, TestConstants.DATASET_PRIMARY_CONTACT_NAME); // Jan Legind
    data.add(PRIMARY_CONTACT_ADDRESS_PARAM, TestConstants.DATASET_PRIMARY_CONTACT_ADDRESS.get(0));
    data.add(PRIMARY_CONTACT_PHONE_PARAM, TestConstants.DATASET_PRIMARY_CONTACT_PHONE.get(0));
    data.add(PRIMARY_CONTACT_DESCRIPTION_PARAM, TestConstants.DATASET_PRIMARY_CONTACT_DESCRIPTION);

    // construct request uri
    String uri = "/registry/resource/" + datasetKey;

    // send POST request with credentials
    ResultActions actions =
        requestTestFixture
            .postRequestUrlEncoded(data, organizationKey, organization.getPassword(), uri)
            .andExpect(status().is2xxSuccessful());

    // parse updated registered Dataset key (UUID)
    LegacyDatasetResponse response =
        requestTestFixture.extractXmlResponse(actions, LegacyDatasetResponse.class);

    assertNotNull(response.getKey(), "Updated Dataset key should be in response");
    assertEquals(datasetKey.toString(), response.getKey());
    assertNotNull(
        response.getOrganisationKey(), "Updated Dataset organizationKey should be in response");
    assertEquals(organizationKey.toString(), response.getOrganisationKey());

    // make some additional assertions that the update was successful
    // retrieve installation anew
    dataset = datasetService.get(datasetKey);

    assertNotNull(dataset, "Dataset should be present");

    // the contact should still have first name "Jan" and last name "Legind"
    assertEquals(1, dataset.getContacts().size());
    Contact updated = dataset.getContacts().get(0);
    assertEquals("Jan", updated.getFirstName());
    assertEquals("Legind", updated.getLastName());
    assertEquals(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT, updated.getType());
  }

  /**
   * The test sends a get all datasets owned by organization (GET) request, the JSON response having
   * at the very least the dataset key, publishing organization key, dataset title, and dataset
   * description.
   */
  @Test
  public void testGetLegacyDatasetsForOrganizationJSON() throws Exception {
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

    // construct request uri
    String uri = "/registry/resource.json?organisationKey=" + organizationKey;

    // send GET request with no credentials
    ResultActions actions =
        requestTestFixture.getRequest(uri).andExpect(status().is2xxSuccessful());

    LegacyDatasetResponseListWrapper responseWrapper =
        requestTestFixture.extractJsonResponse(actions, LegacyDatasetResponseListWrapper.class);

    LegacyDatasetResponse response = responseWrapper.getLegacyDatasetResponses().get(0);
    assertEquals(1, responseWrapper.getLegacyDatasetResponses().size());
    // "key" and "name" expected
    assertEquals(datasetKey.toString(), response.getKey());
    assertEquals(dataset.getPublishingOrganizationKey().toString(), response.getOrganisationKey());
    assertEquals(dataset.getTitle(), response.getName());
  }

  /**
   * The test sends a get all datasets owned by organization (GET) request, the XML response having
   * at the very least the dataset key, publishing organization key, dataset title, and dataset
   * description.
   */
  @Test
  public void testGetLegacyDatasetsForOrganizationXML() throws Exception {
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

    // construct request uri
    String uri = "/registry/resource?organisationKey=" + organizationKey;

    // send GET request with no credentials
    ResultActions actions =
        requestTestFixture.getRequest(uri).andExpect(status().is2xxSuccessful());

    // parse newly registered list of datasets
    LegacyDatasetResponseListWrapper responseWrapper =
        requestTestFixture.extractXmlResponse(actions, LegacyDatasetResponseListWrapper.class);

    LegacyDatasetResponse response = responseWrapper.getLegacyDatasetResponses().get(0);
    assertEquals(datasetKey.toString(), response.getKey());
    assertEquals(organizationKey.toString(), response.getOrganisationKey());
    assertEquals(dataset.getTitle(), response.getName());
  }

  /**
   * The test sends a get dataset (GET) request, the JSON response having all of: key,
   * organisationKey, name, description, nameLanguage, descriptionLanguage, homepageURL,
   * primaryContactType/Name/Email/Phone/Address/Desc.
   */
  @Test
  public void testGetLegacyDatasetJSON() throws Exception {
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

    // add primary contact to Dataset
    Contact c = testDataFactory.newContact();
    c.setType(ContactType.TECHNICAL_POINT_OF_CONTACT);
    datasetService.addContact(dataset.getKey(), c);

    // construct request uri
    String uri = "/registry/resource/" + datasetKey + ".json";

    // send GET request with no credentials
    ResultActions actions =
        requestTestFixture.getRequest(uri).andExpect(status().is2xxSuccessful());

    // JSON object expected, representing single dataset
    LegacyDatasetResponse response =
        requestTestFixture.extractJsonResponse(actions, LegacyDatasetResponse.class);

    // keys "key" and "name" expected
    assertEquals(datasetKey.toString(), response.getKey());
    assertEquals(dataset.getPublishingOrganizationKey().toString(), response.getOrganisationKey());
    assertEquals(dataset.getTitle(), response.getName());
    assertEquals(dataset.getDescription(), response.getDescription());
    assertEquals(dataset.getLanguage().getIso2LetterCode(), response.getNameLanguage());
    assertEquals(dataset.getLanguage().getIso2LetterCode(), response.getDescriptionLanguage());
    assertNotNull(dataset.getHomepage());
    assertEquals(dataset.getHomepage().toString(), response.getHomepageURL());
    assertEquals(TECHNICAL_CONTACT_TYPE, response.getPrimaryContactType());
    assertEquals("Tim Robertson", response.getPrimaryContactName());
    assertEquals("+45 28261487", response.getPrimaryContactPhone());
    assertEquals("trobertson@gbif.org", response.getPrimaryContactEmail());
    assertEquals("Universitetsparken 15", response.getPrimaryContactAddress());
    assertEquals("About 175cm, geeky, scruffy...", response.getPrimaryContactDescription());
  }

  /**
   * The test sends a get dataset (GET) request for a dataset that does not exist. The JSON response
   * having an error message, not a 404.
   */
  @Test
  public void testGetLegacyDatasetNotFoundJSON() throws Exception {
    // construct request uri
    String uri = "/registry/resource/" + UUID.randomUUID() + ".json";

    // send GET request with no credentials
    ResultActions actions =
        requestTestFixture.getRequest(uri).andExpect(status().is2xxSuccessful());

    ErrorResponse response = requestTestFixture.extractJsonResponse(actions, ErrorResponse.class);

    assertEquals("No resource matches the key provided", response.getError());
  }

  /**
   * The test sends a get dataset (GET) request, the XML response having all of: key,
   * organisationKey, name, description, nameLanguage, descriptionLanguage, homepageURL,
   * primaryContactType/Name/Email/Phone/Address/Desc.
   */
  @Test
  public void testGetLegacyDatasetXML() throws Exception {
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

    // add primary contact to Dataset
    Contact c = testDataFactory.newContact();
    c.setType(ContactType.TECHNICAL_POINT_OF_CONTACT);
    datasetService.addContact(dataset.getKey(), c);

    // construct request uri
    String uri = "/registry/resource/" + datasetKey;

    // send GET request with no credentials
    ResultActions actions =
        requestTestFixture.getRequest(uri).andExpect(status().is2xxSuccessful());

    // XML expected, parse Dataset
    LegacyDatasetResponse response =
        requestTestFixture.extractXmlResponse(actions, LegacyDatasetResponse.class);

    assertEquals(dataset.getKey().toString(), response.getKey());
    assertEquals(dataset.getTitle(), response.getName());
    assertEquals(dataset.getLanguage().getIso2LetterCode(), response.getNameLanguage());
    assertEquals(dataset.getDescription(), response.getDescription());
    assertEquals(dataset.getLanguage().getIso2LetterCode(), response.getDescriptionLanguage());
    assertNotNull(dataset.getHomepage());
    assertEquals(dataset.getHomepage().toString(), response.getHomepageURL());
    assertEquals(TECHNICAL_CONTACT_TYPE, response.getPrimaryContactType());
    assertEquals("Tim Robertson", response.getPrimaryContactName());
    assertEquals("trobertson@gbif.org", response.getPrimaryContactEmail());
    assertEquals("Universitetsparken 15", response.getPrimaryContactAddress());
    assertEquals("+45 28261487", response.getPrimaryContactPhone());
    assertEquals("About 175cm, geeky, scruffy...", response.getPrimaryContactDescription());
  }

  /**
   * The test sends a get all datasets owned by organization (GET) request, the JSON response having
   * at the very least the dataset key, publishing organization key, dataset title, and dataset
   * description.
   */
  @Test
  public void testGetLegacyDatasetsForOrganizationThatDoesNotExist() throws Exception {
    // construct request uri using Organization that doesn't exist
    String uri = "/registry/resource.json?organisationKey=" + UUID.randomUUID();

    // send GET request with no credentials
    ResultActions actions =
        requestTestFixture.getRequest(uri).andExpect(status().is2xxSuccessful());

    ErrorResponse response = requestTestFixture.extractJsonResponse(actions, ErrorResponse.class);
    assertEquals("No organisation matches the key provided", response.getError());
  }

  /**
   * Populate a list of name value pairs used in the common ws requests for GBRDS dataset
   * registrations and updates.
   *
   * @param organizationKey organization key
   * @return list of name value pairs
   */
  private MultiValueMap<String, String> buildLegacyDatasetParameters(UUID organizationKey) {
    MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
    // main
    data.add(NAME_PARAM, TestConstants.DATASET_NAME);
    data.add(DOI_PARAM, TestConstants.DOI);
    data.add(NAME_LANGUAGE_PARAM, TestConstants.DATASET_NAME_LANGUAGE);
    data.add(DESCRIPTION_PARAM, TestConstants.DATASET_DESCRIPTION);
    data.add(DESCRIPTION_LANGUAGE_PARAM, TestConstants.DATASET_DESCRIPTION_LANGUAGE);
    data.add(HOMEPAGE_URL_PARAM, TestConstants.DATASET_HOMEPAGE_URL);
    data.add(LOGO_URL_PARAM, TestConstants.DATASET_LOGO_URL);

    // primary contact
    data.add(PRIMARY_CONTACT_TYPE_PARAM, TestConstants.DATASET_PRIMARY_CONTACT_TYPE);
    data.add(PRIMARY_CONTACT_EMAIL_PARAM, TestConstants.DATASET_PRIMARY_CONTACT_EMAIL.get(0));
    data.add(PRIMARY_CONTACT_NAME_PARAM, TestConstants.DATASET_PRIMARY_CONTACT_NAME);
    data.add(PRIMARY_CONTACT_ADDRESS_PARAM, TestConstants.DATASET_PRIMARY_CONTACT_ADDRESS.get(0));
    data.add(PRIMARY_CONTACT_PHONE_PARAM, TestConstants.DATASET_PRIMARY_CONTACT_PHONE.get(0));
    data.add(PRIMARY_CONTACT_DESCRIPTION_PARAM, TestConstants.DATASET_PRIMARY_CONTACT_DESCRIPTION);

    // add additional ipt and organisation parameters
    data.add(ORGANIZATION_KEY_PARAM, organizationKey.toString());
    return data;
  }

  /**
   * Retrieve persisted Legacy (GBRDS) dataset, and make a series of assertions to ensure it has
   * been properly persisted.
   *
   * @param datasetKey installation key (UUID)
   * @param organizationKey installation publishing organization key
   * @return validated installation
   */
  private Dataset validatePersistedLegacyDataset(
      UUID datasetKey, UUID organizationKey, UUID installationKey) {
    // retrieve installation anew
    Dataset dataset = datasetService.get(datasetKey);

    assertNotNull(dataset, "Dataset should be present");
    assertEquals(organizationKey, dataset.getPublishingOrganizationKey());
    assertEquals(installationKey, dataset.getInstallationKey());
    assertEquals(DatasetType.METADATA, dataset.getType());
    assertEquals(TestConstants.DATASET_NAME, dataset.getTitle());
    assertEquals(new DOI(TestConstants.DOI), dataset.getDoi()); // ensure that we handle the parsing
    assertEquals(TestConstants.DATASET_NAME_LANGUAGE, dataset.getLanguage().getIso2LetterCode());
    assertEquals(TestConstants.DATASET_DESCRIPTION, dataset.getDescription());
    assertNotNull(dataset.getHomepage());
    assertNotNull(dataset.getLogoUrl());
    assertEquals(TestConstants.DATASET_HOMEPAGE_URL, dataset.getHomepage().toString());
    assertEquals(TestConstants.DATASET_LOGO_URL, dataset.getLogoUrl().toString());
    assertNotNull(dataset.getCreated());
    assertNotNull(dataset.getModified());
    assertEquals(License.UNSPECIFIED, dataset.getLicense());

    // check dataset's primary contact was properly persisted
    Contact contact = dataset.getContacts().get(0);
    assertNotNull(contact, "Dataset primary contact should be present");
    assertNotNull(contact.getKey());
    assertTrue(contact.isPrimary());
    assertEquals(TestConstants.DATASET_PRIMARY_CONTACT_NAME, contact.getFirstName());
    assertEquals(TestConstants.DATASET_PRIMARY_CONTACT_EMAIL, contact.getEmail());
    assertEquals(TestConstants.DATASET_PRIMARY_CONTACT_PHONE, contact.getPhone());
    assertEquals(TestConstants.DATASET_PRIMARY_CONTACT_ADDRESS, contact.getAddress());
    assertEquals(TestConstants.DATASET_PRIMARY_CONTACT_DESCRIPTION, contact.getDescription());
    assertEquals(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT, contact.getType());
    assertNotNull(contact.getCreated());
    assertNotNull(contact.getCreatedBy());
    assertNotNull(contact.getModified());
    assertNotNull(contact.getModifiedBy());

    return dataset;
  }

  /**
   * Retrieve dataset presumed already to exist, and make a series of assertions to ensure it is
   * valid.
   *
   * @param dataset dataset
   * @param organizationKey publishing organization key
   * @param installationKey installation key
   */
  private void validateExistingDataset(
      Dataset dataset, UUID organizationKey, UUID installationKey) {
    assertNotNull(dataset, "Dataset should be present");
    assertEquals(organizationKey, dataset.getPublishingOrganizationKey());
    assertEquals(installationKey, dataset.getInstallationKey());
    assertEquals(DatasetType.OCCURRENCE, dataset.getType());
    // expected to change on update
    assertNotEquals(TestConstants.DATASET_NAME, dataset.getTitle());
    assertNotEquals(TestConstants.DATASET_DESCRIPTION, dataset.getDescription());
    assertNotEquals(TestConstants.DATASET_HOMEPAGE_URL, dataset.getHomepage());
    assertNotEquals(TestConstants.DATASET_LOGO_URL, dataset.getLogoUrl());
    Date modified = dataset.getModified();
    assertNotNull(modified);
    String modifiedBy = dataset.getModifiedBy();
    assertNotNull(modifiedBy);
    // The existing Legacy (GBRDS) dataset was assigned CC-BY-NC 4.0 license.
    // It isn't overwritten by update, because Legacy (GBRDS) datasets can be only be assigned
    // license via EML metadata
    // document, and parsed/set during crawling
    assertEquals(License.CC_BY_NC_4_0, dataset.getLicense());

    assertTrue(dataset.getContacts().isEmpty());
    assertTrue(dataset.getEndpoints().isEmpty());
    // not expected to change
    assertEquals(Datasets.DATASET_LANGUAGE, dataset.getLanguage());
    assertEquals(Datasets.DATASET_RIGHTS, dataset.getRights());
    // per https://github.com/gbif/registry/issues/4, Citation is now generated
    assertNotNull(dataset.getCitation());
    assertEquals(
        Datasets.buildExpectedCitation(dataset, Organizations.ORGANIZATION_TITLE)
            .getCitation()
            .getText(),
        dataset.getCitation().getText());
    assertEquals(Datasets.DATASET_ABBREVIATION, dataset.getAbbreviation());
    assertEquals(Datasets.DATASET_ALIAS, dataset.getAlias());
  }
}
