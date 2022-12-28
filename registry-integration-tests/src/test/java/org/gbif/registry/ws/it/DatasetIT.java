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
package org.gbif.registry.ws.it;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.model.registry.search.DatasetSearchRequest;
import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.service.registry.DatasetSearchService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.MaintenanceUpdateFrequency;
import org.gbif.api.vocabulary.MetadataType;
import org.gbif.registry.search.dataset.indexing.DatasetRealtimeIndexer;
import org.gbif.registry.search.test.DatasetSearchUpdateUtils;
import org.gbif.registry.search.test.ElasticsearchInitializer;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.Datasets;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.client.DatasetClient;
import org.gbif.registry.ws.client.InstallationClient;
import org.gbif.registry.ws.client.NodeClient;
import org.gbif.registry.ws.client.OrganizationClient;
import org.gbif.registry.ws.resources.DatasetResource;
import org.gbif.utils.file.FileUtils;
import org.gbif.ws.NotFoundException;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.ibatis.io.Resources;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gbif.registry.test.Datasets.buildExpectedProcessedProperties;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is parameterized to run the same test routines for the following:
 *
 * <ol>
 *   <li>The persistence layer
 *   <li>The WS service layer
 *   <li>The WS service client layer
 * </ol>
 */
@SpringBootTest(
    classes = RegistryIntegrationTestsConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DatasetIT extends NetworkEntityIT<Dataset> {

  private final DatasetSearchService searchService;
  private final OrganizationService organizationResource;
  private final OrganizationService organizationClient;
  private final NodeService nodeResource;
  private final NodeService nodeClient;
  private final InstallationService installationResource;
  private final InstallationService installationClient;
  private final DatasetRealtimeIndexer datasetRealtimeIndexer;
  private final TestDataFactory testDataFactory;

  @RegisterExtension
  ElasticsearchInitializer elasticsearchInitializer = new ElasticsearchInitializer(esServer);

  @Autowired
  public DatasetIT(
      DatasetService service,
      DatasetSearchService searchService,
      OrganizationService organizationResource,
      NodeService nodeResource,
      InstallationService installationResource,
      DatasetRealtimeIndexer datasetRealtimeIndexer,
      @Nullable SimplePrincipalProvider principalProvider,
      TestDataFactory testDataFactory,
      EsManageServer esServer,
      KeyStore keyStore,
      @LocalServerPort int localServerPort) {
    super(
        service,
        localServerPort,
        keyStore,
        DatasetClient.class,
        principalProvider,
        testDataFactory,
        esServer);
    this.searchService = searchService;
    this.organizationResource = organizationResource;
    this.organizationClient = prepareClient(localServerPort, keyStore, OrganizationClient.class);
    this.nodeResource = nodeResource;
    this.nodeClient = prepareClient(localServerPort, keyStore, NodeClient.class);
    this.installationResource = installationResource;
    this.installationClient = prepareClient(localServerPort, keyStore, InstallationClient.class);
    this.datasetRealtimeIndexer = datasetRealtimeIndexer;
    this.testDataFactory = testDataFactory;
  }

  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testCreateDoi(ServiceType serviceType) {
    DatasetService service = (DatasetService) getService(serviceType);
    Dataset d = newEntity(serviceType);
    service.create(d);
    assertEquals(Datasets.DATASET_DOI, d.getDoi());

    d = newEntity(serviceType);
    d.setDoi(null);
    UUID key = service.create(d);
    d = service.get(key);
    assertNotEquals(Datasets.DATASET_DOI, d.getDoi());
    assertEquals(DOI.TEST_PREFIX, d.getDoi().getPrefix());
  }

  /** Override creation to add process properties. */
  @Override
  protected Dataset create(Dataset orig, ServiceType serviceType) {
    return create(orig, serviceType, buildExpectedProcessedProperties(orig));
  }

  @Override
  protected Dataset duplicateForCreateAsEditorTest(Dataset entity) throws Exception {
    Dataset duplicate = (Dataset) BeanUtils.cloneBean(entity);
    duplicate.setPublishingOrganizationKey(entity.getPublishingOrganizationKey());
    duplicate.setInstallationKey(entity.getInstallationKey());
    return duplicate;
  }

  @Override
  protected UUID keyForCreateAsEditorTest(Dataset entity) {
    return organizationResource.get(entity.getPublishingOrganizationKey()).getEndorsingNodeKey();
  }

  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testConstituents(ServiceType serviceType) {
    DatasetService service = (DatasetService) getService(serviceType);
    Dataset parent = newAndCreate(serviceType);
    for (int id = 0; id < 10; id++) {
      Dataset constituent = newEntity(serviceType);
      constituent.setParentDatasetKey(parent.getKey());
      constituent.setType(parent.getType());
      create(constituent, serviceType);
    }

    assertEquals(10, service.get(parent.getKey()).getNumConstituents());
  }

  // Easier to test this here than OrganizationIT due to our utility dataset factory
  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testHostedByList(ServiceType serviceType) {
    OrganizationService organizationService =
        getService(serviceType, organizationResource, organizationClient);
    InstallationService installationService =
        getService(serviceType, installationResource, installationClient);

    Dataset dataset = newAndCreate(serviceType);
    Installation i = installationService.get(dataset.getInstallationKey());
    assertNotNull(i, "Dataset should have an installation");
    PagingResponse<Dataset> published =
        organizationService.publishedDatasets(i.getOrganizationKey(), new PagingRequest());
    PagingResponse<Dataset> hosted =
        organizationService.hostedDatasets(i.getOrganizationKey(), new PagingRequest());
    assertEquals(
        1, published.getResults().size(), "This installation should have only 1 published dataset");
    assertTrue(
        hosted.getResults().isEmpty(), "This organization should not have any hosted datasets");
  }

  // Easier to test this here than OrganizationIT due to our utility dataset factory
  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testPublishedByList(ServiceType serviceType) {
    OrganizationService organizationService =
        getService(serviceType, organizationResource, organizationClient);

    Dataset dataset = newAndCreate(serviceType);
    PagingResponse<Dataset> published =
        organizationService.publishedDatasets(
            dataset.getPublishingOrganizationKey(), new PagingRequest());
    assertEquals(1, published.getResults().size(), "The organization should have only 1 dataset");
    assertEquals(
        published.getResults().get(0).getKey(),
        dataset.getKey(),
        "The organization should publish the dataset created");

    assertEquals(
        1,
        organizationService.get(dataset.getPublishingOrganizationKey()).getNumPublishedDatasets(),
        "The organization should have 1 dataset count");
  }

  // Easier to test this here than InstallationIT due to our utility dataset factory
  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testHostedByInstallationList(ServiceType serviceType) {
    InstallationService installationService =
        getService(serviceType, installationResource, installationClient);

    Dataset dataset = newAndCreate(serviceType);
    Installation i = installationService.get(dataset.getInstallationKey());
    assertNotNull(i, "Dataset should have an installation");
    PagingResponse<Dataset> hosted =
        installationService.getHostedDatasets(dataset.getInstallationKey(), new PagingRequest());
    assertEquals(
        1, hosted.getResults().size(), "This installation should have only 1 hosted dataset");
    assertEquals(Long.valueOf(1), hosted.getCount(), "Paging response counts are not being set");
    assertEquals(
        hosted.getResults().get(0).getKey(),
        dataset.getKey(),
        "The hosted installation should serve the dataset created");
  }

  private void assertAll(Long expected) {
    // Elasticsearch updates are asynchronous
    DatasetSearchUpdateUtils.awaitUpdates(datasetRealtimeIndexer, esServer);

    DatasetSearchRequest req = new DatasetSearchRequest();
    req.setQ("*");
    SearchResponse<DatasetSearchResult, DatasetSearchParameter> resp = searchService.search(req);
    assertNotNull(resp.getCount());
    System.out.println(resp.getCount());
    System.out.println(resp);
    assertEquals(expected, resp.getCount(), "Elasticsearch docs not as expected");
  }

  /**
   * Utility to verify that after waiting for Elasticsearch to update, the given query returns the
   * expected count of results.
   */
  private void assertSearch(Country publishingCountry, Country country, int expected) {
    // Elasticsearch updates are asynchronous
    DatasetSearchUpdateUtils.awaitUpdates(datasetRealtimeIndexer, esServer);

    DatasetSearchRequest req = new DatasetSearchRequest();
    if (country != null) {
      req.addCountryFilter(country);
    }
    if (publishingCountry != null) {
      req.addPublishingCountryFilter(publishingCountry);
    }
    SearchResponse<DatasetSearchResult, DatasetSearchParameter> resp = searchService.search(req);
    assertNotNull(resp.getCount());
    assertEquals(
        Long.valueOf(expected),
        resp.getCount(),
        "Elasticsearch does not have the expected number of results for country["
            + country
            + "] and publishingCountry["
            + publishingCountry
            + "]");
  }

  private Dataset newEntity(@Nullable Country publisherCountry, ServiceType serviceType) {
    NodeService nodeService = getService(serviceType, nodeResource, nodeClient);
    OrganizationService organizationService =
        getService(serviceType, organizationResource, organizationClient);
    InstallationService installationService =
        getService(serviceType, installationResource, installationClient);

    // endorsing node for the organization
    UUID nodeKey = nodeService.create(testDataFactory.newNode());
    // publishing organization (required field)
    Organization o = testDataFactory.newOrganization(nodeKey);
    if (publisherCountry != null) {
      o.setCountry(publisherCountry);
    }
    UUID organizationKey = organizationService.create(o);

    Installation i = testDataFactory.newInstallation(organizationKey);
    UUID installationKey = installationService.create(i);

    return newEntity(organizationKey, installationKey);
  }

  @Override
  protected Dataset newEntity(ServiceType serviceType) {
    return newEntity(null, serviceType);
  }

  private Dataset newEntity(UUID organizationKey, UUID installationKey) {
    return testDataFactory.newDataset(organizationKey, installationKey);
  }

  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testDatasetHTMLSanitizer(ServiceType serviceType) {
    Dataset dataset = newEntity(serviceType);
    dataset.setDescription(
        "<h1 style=\"color:red\">headline</h1><br/>"
            + "<p>paragraph with <a href=\"http://www.gbif.org\">link</a> and <em>italics</em></p>"
            + "<script>//my script</script>"
            + "<iframe src=\"perdu.com\">");

    String expectedParagraph =
        "<h1>headline</h1><br /><p>paragraph with <a href=\"http://www.gbif.org\">link</a> and <em>italics</em></p>";

    Map<String, Object> processProperties = Datasets.buildExpectedProcessedProperties(dataset);
    processProperties.put("description", expectedParagraph);
    dataset = create(dataset, serviceType, processProperties);
    assertEquals(expectedParagraph, dataset.getDescription());
  }

  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testCitation(ServiceType serviceType) {
    DatasetService service = (DatasetService) getService(serviceType);
    Dataset dataset = newEntity(serviceType);
    dataset = create(dataset, serviceType);
    dataset = service.get(dataset.getKey());
    assertNotNull(dataset.getCitation(), "Citation should never be null");

    assertEquals("ABC", dataset.getCitation().getIdentifier());
    // original citation not preserved, we generate one
    assertNotEquals("This is a citation text", dataset.getCitation().getText());

    // update it
    dataset.getCitation().setIdentifier("doi:456");
    dataset.getCitation().setText("GOD publishing, volume 123");
    service.update(dataset);
    dataset = service.get(dataset.getKey());
    assertNotNull(dataset.getCitation());
    assertEquals("doi:456", dataset.getCitation().getIdentifier());
    // original citation not preserved, we generate one
    assertNotEquals("GOD publishing, volume 123", dataset.getCitation().getText());
    // generated citation contains the DOI
    assertTrue(dataset.getCitation().getText().contains("xsd123"));

    // setting to null should make it the default using the org:dataset titles
    dataset.getCitation().setText(null);
    service.update(dataset);
    dataset = service.get(dataset.getKey());
    assertNotNull(dataset.getCitation());
    assertEquals("doi:456", dataset.getCitation().getIdentifier());
    // original citation not preserved, we generate one
    assertNotEquals(
        "The BGBM: Pontaurus needs more than 255 characters for it's title. It is a very, very, very, very long title in the German language. Word by word and character by character it's exact title is: \"Vegetationskundliche Untersuchungen in der Hochgebirgsregion der Bolkar Daglari & Aladaglari, Türkei\"",
        dataset.getCitation().getText());
  }

  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testDoiChanges(ServiceType serviceType) {
    DatasetService service = (DatasetService) getService(serviceType);
    final DOI external1 = new DOI("10.9999/nonGbif");
    final DOI external2 = new DOI("10.9999/nonGbif2");
    // we use the test prefix in tests for GBIF DOIs, see registry-test.properties
    final DOI gbif2 = new DOI("10.21373/sthelse");

    Dataset src = newEntity(serviceType);
    src.setDoi(external1);
    final UUID key = create(src, serviceType).getKey();
    Dataset dataset = service.get(key);
    assertEquals(external1, dataset.getDoi());
    assertEquals(0, service.listIdentifiers(key).size());

    dataset.setDoi(null);
    service.update(dataset);
    dataset = service.get(key);
    assertNotNull(dataset.getDoi(), "DOI should never be null");
    assertNotEquals(dataset.getDoi(), external1);
    final DOI originalGBIF = dataset.getDoi();
    assertThat(service.listIdentifiers(key))
        .hasSize(1)
        .extracting("identifier")
        .contains(external1.toString());

    dataset.setDoi(external1);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(external1, dataset.getDoi());
    assertThat(service.listIdentifiers(key))
        .hasSize(1)
        .extracting("identifier")
        .contains(originalGBIF.toString());

    dataset.setDoi(external2);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(external2, dataset.getDoi());
    assertThat(service.listIdentifiers(key))
        .hasSize(2)
        .extracting("identifier")
        .contains(originalGBIF.toString(), external1.toString());

    dataset.setDoi(null);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(originalGBIF, dataset.getDoi());
    assertThat(service.listIdentifiers(key))
        .hasSize(2)
        .extracting("identifier")
        .contains(external1.toString(), external2.toString());

    dataset.setDoi(gbif2);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(gbif2, dataset.getDoi());
    assertThat(service.listIdentifiers(key))
        .hasSize(3)
        .extracting("identifier")
        .contains(external1.toString(), external2.toString(), originalGBIF.toString());

    dataset.setDoi(external1);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(external1, dataset.getDoi());
    assertThat(service.listIdentifiers(key))
        .hasSize(3)
        .extracting("identifier")
        .contains(gbif2.toString(), external2.toString(), originalGBIF.toString());
  }

  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testLicenseChanges(ServiceType serviceType) {
    DatasetService service = (DatasetService) getService(serviceType);
    Dataset src = newEntity(serviceType);

    // start with dataset with null license
    src.setLicense(null);

    // register dataset
    final UUID key = create(src, serviceType).getKey();

    // ensure default license CC-BY 4.0 was assigned
    Dataset dataset = service.get(key);
    assertEquals(License.CC_BY_4_0, dataset.getLicense());

    // try updating dataset, setting license to NULL - ensure original license was preserved
    dataset.setLicense(null);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(License.CC_BY_4_0, dataset.getLicense());

    // try updating dataset with different, less restrictive license CC0 1.0 - ensure license was
    // replaced
    dataset.setLicense(License.CC0_1_0);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(License.CC0_1_0, dataset.getLicense());

    // try updating dataset with an UNSUPPORTED license - ensure original license CC0 1.0 was
    // preserved
    dataset.setLicense(License.UNSUPPORTED);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(License.CC0_1_0, dataset.getLicense());

    // try updating dataset with an UNSPECIFIED license - ensure original license CC0 1.0 was
    // preserved
    dataset.setLicense(License.UNSPECIFIED);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(License.CC0_1_0, dataset.getLicense());
  }

  /**
   * Test calls DatasetResource.updateFromPreferredMetadata directly, to ensure it updates the
   * dataset by reinterpreting its preferred metadata document. In particular the test ensures the
   * dataset license is updated properly as per the metadata document.
   */
  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(
      value = ServiceType.class,
      names = {"RESOURCE"})
  public void testUpdateFromPreferredMetadata(ServiceType serviceType) throws IOException {
    DatasetService service = (DatasetService) getService(serviceType);
    Dataset src = newEntity(serviceType);

    // start with dataset with CC0 license
    src.setLicense(License.CC0_1_0);

    // register dataset
    final UUID key = create(src, serviceType).getKey();

    // ensure license CC0 was assigned
    Dataset dataset = service.get(key);
    assertEquals(License.CC0_1_0, dataset.getLicense());

    // insert metadata document, which sets license to more restrictive license CC-BY - ensure
    // license was replaced
    InputStream is = Resources.getResourceAsStream("metadata/sample-v1.1.xml");
    service.insertMetadata(key, is);
    dataset = service.get(key);
    assertEquals(License.CC_BY_4_0, dataset.getLicense());

    // update dataset, overwritting license back to CC0
    dataset.setLicense(License.CC0_1_0);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(License.CC0_1_0, dataset.getLicense());

    // last, update dataset from preferred metadata document, ensuring license gets reset to CC-BY

    ((DatasetResource) service).updateFromPreferredMetadata(key, "DatasetIT");
    dataset = service.get(key);
    assertNotNull(dataset);
    assertEquals(License.CC_BY_4_0, dataset.getLicense());
  }

  /**
   * Test checks behaviour updating Citation with valid and invalid identifier. In the database,
   * there is a min length 1 character constraint on Dataset.citation_identifier.
   */
  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testDatasetCitationIdentifierConstraint(ServiceType serviceType) {
    DatasetService service = (DatasetService) getService(serviceType);
    Dataset src = newEntity(serviceType);

    // register dataset
    final UUID key = create(src, serviceType).getKey();

    Dataset dataset = service.get(key);
    assertNotNull(dataset.getCitation());

    // set Citation identifier to null
    Citation c = dataset.getCitation();
    c.setIdentifier(null);
    dataset.setCitation(c);
    service.update(dataset);

    // check update succeeds
    dataset = service.get(key);
    assertNotNull(dataset.getCitation());
    // we use the generated citation
    assertNotEquals("This is a citation text", dataset.getCitation().getText());
    assertNull(dataset.getCitation().getIdentifier());

    // set Citation identifier to single character
    c = dataset.getCitation();
    c.setIdentifier("");
    dataset.setCitation(c);

    // update dataset...
    ConstraintViolationException exception = null;
    try {
      service.update(dataset);
    } catch (ConstraintViolationException e) {
      exception = e;
    }
    // /...and check it fails, however, constraint violation can only be thrown by web service
    // because client
    // trims Citation fields via StringTrimInterceptor
    if (service instanceof DatasetResource) {
      assertNotNull(exception);
    }
  }

  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testMaintenanceUpdateFrequencyChanges(ServiceType serviceType) {
    DatasetService service = (DatasetService) getService(serviceType);
    Dataset src = newEntity(serviceType);
    assertNull(src.getMaintenanceUpdateFrequency());
    final UUID key = create(src, serviceType).getKey();
    Dataset dataset = service.get(key);
    assertNull(src.getMaintenanceUpdateFrequency());

    // try updating maintenanceUpdateFrequency - ensure value persisted
    dataset.setMaintenanceUpdateFrequency(MaintenanceUpdateFrequency.AS_NEEDED);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(MaintenanceUpdateFrequency.AS_NEEDED, dataset.getMaintenanceUpdateFrequency());

    // try updating maintenanceUpdateFrequency again - ensure value replaced
    dataset.setMaintenanceUpdateFrequency(MaintenanceUpdateFrequency.BIANNUALLY);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(MaintenanceUpdateFrequency.BIANNUALLY, dataset.getMaintenanceUpdateFrequency());
  }

  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void test404(ServiceType serviceType) {
    if (serviceType == ServiceType.CLIENT) {
      assertNull(getService(serviceType).get(UUID.randomUUID()));
    } else {
      assertThrows(NotFoundException.class, () -> getService(serviceType).get(UUID.randomUUID()));
    }
  }

  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testMetadata(ServiceType serviceType) throws IOException {
    DatasetService service = (DatasetService) getService(serviceType);
    Dataset d1 = create(newEntity(serviceType), serviceType);
    assertEquals(License.CC_BY_NC_4_0, d1.getLicense());
    List<Metadata> metadata = service.listMetadata(d1.getKey(), MetadataType.EML);
    assertEquals(0, metadata.size(), "No EML uploaded yes");

    // upload a valid EML document (without a machine readable license)
    service.insertMetadata(d1.getKey(), FileUtils.classpathStream("metadata/sample.xml"));

    // verify dataset was updated from parsed document
    Dataset d2 = service.get(d1.getKey());
    assertNotEquals(d1, d2, "Dataset should have changed after metadata was uploaded");
    assertEquals("Tanzanian Entomological Collection", d2.getTitle());
    assertEquals(d1.getCreated(), d2.getCreated(), "Created data should not change");
    assertNotNull(d1.getModified());
    assertNotNull(d2.getModified());
    assertTrue(
        d1.getModified().before(d2.getModified()), "Dataset modification date should change");

    // verify license stayed the same, because no machine readable license was detected in EML
    // document
    assertEquals(License.CC_BY_NC_4_0, d2.getLicense());

    // verify EML document was stored successfully
    metadata = service.listMetadata(d1.getKey(), MetadataType.EML);
    assertEquals(1, metadata.size(), "Exactly one EML uploaded");
    assertEquals(MetadataType.EML, metadata.get(0).getType(), "Wrong metadata type");

    // check number of stored DC documents
    metadata = service.listMetadata(d1.getKey(), MetadataType.DC);
    assertTrue(metadata.isEmpty(), "No Dublin Core uplaoded yet");

    // upload subsequent DC document which has less priority than the previous EML document
    service.insertMetadata(d1.getKey(), FileUtils.classpathStream("metadata/worms_dc.xml"));

    // verify dataset has NOT changed
    Dataset d3 = service.get(d1.getKey());
    assertEquals("Tanzanian Entomological Collection", d3.getTitle());
    assertEquals(d1.getCreated(), d3.getCreated(), "Created data should not change");

    // verify DC document was stored successfully
    metadata = service.listMetadata(d1.getKey(), MetadataType.DC);
    assertEquals(1, metadata.size(), "Exactly one DC uploaded");
    assertEquals(MetadataType.DC, metadata.get(0).getType(), "Wrong metadata type");

    // upload 2nd EML doc (with a machine readable license), which has higher priority than the
    // previous EML doc
    service.insertMetadata(d1.getKey(), FileUtils.classpathStream("metadata/sample-v1.1.xml"));

    // verify dataset was updated from parsed document
    Dataset d4 = service.get(d1.getKey());
    assertEquals("Sample Metadata RLS", d4.getTitle());

    // verify license was updated because CC-BY 4.0 license was detected in EML document
    assertEquals(License.CC_BY_4_0, d4.getLicense());

    // verify EML document replaced EML docuemnt of less priority
    metadata = service.listMetadata(d1.getKey(), MetadataType.EML);
    assertEquals(1, metadata.size(), "Exactly one EML uploaded");

    // upload 3rd EML doc (with a machine readable UNSUPPORTED license), which has higher priority
    // than the previous EML doc
    service.insertMetadata(
        d1.getKey(), FileUtils.classpathStream("metadata/sample-v1.1-unsupported-license.xml"));

    // verify dataset was updated from parsed document
    Dataset d5 = service.get(d1.getKey());
    assertEquals("Sample Metadata RLS (2)", d5.getTitle());

    // verify license was NOT updated because UNSUPPORTED license was detected in EML document (only
    // supported license
    // can overwrite existing license)
    assertEquals(License.CC_BY_4_0, d5.getLicense());

    // verify EML document replaced EML document of less priority
    metadata = service.listMetadata(d1.getKey(), MetadataType.EML);
    assertEquals(1, metadata.size(), "Exactly one EML uploaded");
    int lastEmlMetadataDocKey = metadata.get(0).getKey();

    // upload exact same EML doc again, but make sure it does NOT update dataset!
    Dataset lastUpated = service.get(d1.getKey());
    service.insertMetadata(
        d1.getKey(), FileUtils.classpathStream("metadata/sample-v1.1-unsupported-license.xml"));

    // verify dataset was NOT updated from parsed document
    Dataset d6 = service.get(d1.getKey());
    assertNotNull(d6.getModified());
    assertNotNull(lastUpated.getModified());
    assertEquals(0, d6.getModified().compareTo(lastUpated.getModified()));

    // verify EML document NOT replaced
    List<Metadata> metadata2 = service.listMetadata(d1.getKey(), MetadataType.EML);
    int emlMetadataDocKey = metadata2.get(0).getKey();
    assertEquals(lastEmlMetadataDocKey, emlMetadataDocKey);

    // verify original EML document can be retrieved by WS request (verify POR-3170 fixed)
    InputStream persistedDocument = service.getMetadataDocument(emlMetadataDocKey);
    String persistedDocumentContent =
        CharStreams.toString(new InputStreamReader(persistedDocument, Charsets.UTF_8));
    InputStream originalDocument =
        FileUtils.classpathStream("metadata/sample-v1.1-unsupported-license.xml");
    String originalDocumentContent =
        CharStreams.toString(new InputStreamReader(originalDocument, Charsets.UTF_8));
    assertEquals(originalDocumentContent, persistedDocumentContent);
  }

  /** Test that uploading the same document repeatedly does not change the dataset. */
  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testMetadataDuplication(ServiceType serviceType) throws IOException {
    DatasetService service = (DatasetService) getService(serviceType);
    Dataset d1 = newAndCreate(serviceType);
    List<Metadata> m1 = service.listMetadata(d1.getKey(), MetadataType.EML);

    // upload a valid EML doc
    service.insertMetadata(d1.getKey(), FileUtils.classpathStream("metadata/sample.xml"));

    // verify our dataset has changed
    Dataset d2 = service.get(d1.getKey());
    assertNotEquals(d1, d2, "Dataset should have changed after metadata was uploaded");
    List<Metadata> m2 = service.listMetadata(d1.getKey(), MetadataType.EML);
    assertNotEquals(m1, m2, "Dataset metadata should have changed after metadata was uploaded");

    // upload the doc a second time - it should not update the metadata
    service.insertMetadata(d1.getKey(), FileUtils.classpathStream("metadata/sample.xml"));
    List<Metadata> m3 = service.listMetadata(d1.getKey(), MetadataType.EML);
    assertEquals(
        m2,
        m3,
        "Dataset metadata should not have changed after same metadata document was uploaded");
  }

  @Execution(ExecutionMode.CONCURRENT)
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void createDatasetsWithInvalidUri(ServiceType serviceType) {
    DatasetService service = (DatasetService) getService(serviceType);
    Dataset d = newEntity(serviceType);
    d.setHomepage(URI.create("file:/test.txt"));
    assertThrows(ValidationException.class, () -> service.create(d));
  }

  private void createCountryDatasets(
      ServiceType serviceType, Country publishingCountry, int number) {
    createCountryDatasets(
        DatasetType.OCCURRENCE, serviceType, publishingCountry, number, (Country) null);
  }

  private void createCountryDatasets(
      DatasetType type,
      ServiceType serviceType,
      Country publishingCountry,
      int number,
      Country... countries) {
    DatasetService service = (DatasetService) getService(serviceType);
    OrganizationService organizationService =
        getService(serviceType, organizationResource, organizationClient);

    Dataset d = addCountryCoverage(newEntity(serviceType), countries);
    d.setType(type);
    service.create(d);

    // assign a controlled country based organization
    Organization org = organizationService.get(d.getPublishingOrganizationKey());
    org.setCountry(publishingCountry);
    organizationService.update(org);

    // create datasets for it
    for (int x = 1; x < number; x++) {
      d = addCountryCoverage(newEntity(org.getKey(), d.getInstallationKey()));
      d.setType(type);
      service.create(d);
    }
  }

  /**
   * Create a number of new Datasets, having a particular dataset type.
   *
   * @param type dataset type
   * @param number amount of datasets to create
   */
  private void createDatasetsWithType(DatasetType type, ServiceType serviceType, int number) {
    // create datasets for it
    for (int x = 0; x < number; x++) {
      Dataset d = newEntity(serviceType);
      d.setType(type);
      getService(serviceType).create(d);
    }
  }

  private Dataset addCountryCoverage(Dataset d, Country... countries) {
    if (countries != null) {
      for (Country c : countries) {
        if (c != null) {
          d.getCountryCoverage().add(c);
        }
      }
    }
    return d;
  }

  /** Create a new instance of Dataset, store it using the create method. */
  private Dataset newAndCreate(ServiceType serviceType) {
    Dataset newDataset = newEntity(serviceType);
    return create(newDataset, serviceType);
  }
}
