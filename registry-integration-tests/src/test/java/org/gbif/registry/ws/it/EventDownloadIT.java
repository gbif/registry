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

import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_ADMIN;
import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.AccessControlException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import javax.validation.ValidationException;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.event.search.EventSearchParameter;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.occurrence.Download.Status;
import org.gbif.api.model.occurrence.DownloadFormat;
import org.gbif.api.model.occurrence.DownloadType;
import org.gbif.api.model.occurrence.PredicateDownloadRequest;
import org.gbif.api.model.predicate.EqualsPredicate;
import org.gbif.api.model.registry.CountryOccurrenceDownloadUsage;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.OrganizationOccurrenceDownloadUsage;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.CountryUsageSortField;
import org.gbif.api.vocabulary.DatasetUsageSortField;
import org.gbif.api.vocabulary.Extension;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.OrganizationUsageSortField;
import org.gbif.api.vocabulary.SortOrder;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.client.EventDownloadClient;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;

/**
 * Runs tests for the {@link OccurrenceDownloadService} event implementations. This is parameterized
 * to run the same test routines for the following:
 *
 * <ol>
 *   <li>The persistence layer
 *   <li>The WS service layer
 *   <li>The WS service client layer
 * </ol>
 */
public class EventDownloadIT extends BaseItTest {

  private static final String DEFAULT_SOURCE = "testSource";

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer();

  private TestDataFactory testDataFactory;
  private final OccurrenceDownloadService eventDownloadResource;
  private final EventDownloadClient eventDownloadClient;

  // The following services are required to create dataset instances
  private final DatasetService datasetService;
  private final OrganizationService organizationService;
  private final NodeService nodeService;
  private final InstallationService installationService;
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final JsonNode machineDescription = mapper.createObjectNode().put("key", "value");

  @Autowired
  public EventDownloadIT(
      OccurrenceDownloadService eventDownloadResource,
      OrganizationService organizationService,
      DatasetService datasetService,
      NodeService nodeService,
      InstallationService installationService,
      SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer,
      @LocalServerPort int localServerPort,
      TestDataFactory testDataFactory,
      KeyStore keyStore) {
    super(simplePrincipalProvider, esServer);
    this.eventDownloadResource = eventDownloadResource;
    this.organizationService = organizationService;
    this.datasetService = datasetService;
    this.nodeService = nodeService;
    this.installationService = installationService;
    this.eventDownloadClient = prepareClient(localServerPort, keyStore, EventDownloadClient.class);
    this.testDataFactory = testDataFactory;
  }

  /**
   * Creates {@link Download} instance with test data using a predicate request. The key is
   * generated randomly using the class java.util.UUID. The instance generated should be ready and
   * valid to be persisted.
   */
  protected static Download getTestInstanceDownload() {
    Download download = new Download();
    download.setKey(UUID.randomUUID().toString());
    download.setStatus(Status.PREPARING);
    download.setDoi(new DOI("doi:10.1234/1ASCDU"));
    download.setDownloadLink("testUrl");
    download.setEraseAfter(Date.from(OffsetDateTime.now(ZoneOffset.UTC).plusMonths(6).toInstant()));
    download.setSource(DEFAULT_SOURCE);

    return download;
  }

  /**
   * Creates {@link Download} instance with test data using a predicate request. The key is
   * generated randomly using the class java.util.UUID. The instance generated should be ready and
   * valid to be persisted.
   */
  protected static Download getTestInstancePredicateDownload() {
    Download download = getTestInstanceDownload();
    download.setRequest(
        new PredicateDownloadRequest(
            new EqualsPredicate(EventSearchParameter.TAXON_KEY, "212", false),
            TEST_ADMIN,
            Collections.singleton("downloadtest@gbif.org"),
            true,
            DownloadFormat.DWCA,
            DownloadType.EVENT,
            "testDescription",
            machineDescription,
            Collections.singleton(Extension.AUDUBON),
            null,
            null));
    return download;
  }

  /**
   * Creates {@link Download} instance with test data using a predicate request. The key is
   * generated randomly using the class java.util.UUID. The instance generated should be ready and
   * valid to be persisted.
   */
  protected static Download getTestInstanceNullPredicateDownload() {
    Download download = getTestInstanceDownload();
    download.setRequest(
        new PredicateDownloadRequest(
            null,
            TEST_ADMIN,
            Collections.singleton("downloadtest@gbif.org"),
            true,
            DownloadFormat.DWCA,
            DownloadType.EVENT,
            "testDescription",
            machineDescription,
            Collections.singleton(Extension.AUDUBON),
            null,
            null));
    return download;
  }

  /** Persists a valid {@link Download} instance. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testCreate(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, eventDownloadResource, eventDownloadClient);
    service.create(getTestInstancePredicateDownload());
  }

  /** Persists a valid {@link Download} instance with null predicates. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testCreateWithNullPredicate(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, eventDownloadResource, eventDownloadClient);
    service.create(getTestInstanceNullPredicateDownload());
  }

  /** Tests the create and get(key) methods. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testCreateAndGet(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, eventDownloadResource, eventDownloadClient);
    Download eventDownload = getTestInstancePredicateDownload();
    service.create(eventDownload);
    Download eventDownload2 = service.get(eventDownload.getKey());
    assertNotNull(eventDownload2);
    assertEquals(eventDownload.getRequest(), eventDownload2.getRequest());
    assertNull(eventDownload2.getNumberOrganizations());
    assertNull(eventDownload2.getNumberPublishingCountries());
  }

  /** Tests the create and get(key) methods for null predicate. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testCreateAndGetNullPredicate(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, eventDownloadResource, eventDownloadClient);
    Download eventDownload = getTestInstanceNullPredicateDownload();
    service.create(eventDownload);
    Download eventDownload2 = service.get(eventDownload.getKey());
    assertNotNull(eventDownload2);
    assertEquals(eventDownload.getRequest(), eventDownload2.getRequest());
  }

  /**
   * Tests that sensitive fields (description, creator) are correctly hidden when fetching a
   * download with a null predicate and a predicate-based download.
   */
  @Test
  public void testCreateAndGetHiddenSensitiveFields() {
    OccurrenceDownloadService service =
        getService(ServiceType.RESOURCE, eventDownloadResource, eventDownloadClient);

    Download nullPredicateDownload = getTestInstanceNullPredicateDownload();
    service.create(nullPredicateDownload);

    Download predicateDownload = getTestInstancePredicateDownload();
    service.create(predicateDownload);

    // Hide the sensitive fields if the user is not an admin
    resetSecurityContext(TEST_USER, UserRole.USER);
    Download retrievedNullPredicate = service.get(nullPredicateDownload.getKey());
    assertNull(
        retrievedNullPredicate.getRequest().getDescription(), "Description should be hidden");
    assertNull(retrievedNullPredicate.getRequest().getCreator(), "Creator should be hidden");

    Download retrievedPredicateDownload = service.get(predicateDownload.getKey());
    assertNull(
        retrievedPredicateDownload.getRequest().getDescription(),
        "Description should be hidden for predicate download");
    assertNull(retrievedPredicateDownload.getRequest().getCreator(), "Creator should be hidden");

    // Show all fields if the user is an admin
    resetSecurityContext(TEST_ADMIN, UserRole.GRSCICOLL_ADMIN);
    Download retrievedNullPredicate3 = service.get(nullPredicateDownload.getKey());
    assertNotNull(
        retrievedNullPredicate3.getRequest().getDescription(), "Description should not be hidden");
    assertNotNull(
        retrievedNullPredicate3.getRequest().getCreator(), "Creator should not be hidden");

    Download retrievedPredicateDownload4 = service.get(predicateDownload.getKey());
    assertNotNull(
        retrievedPredicateDownload4.getRequest().getDescription(),
        "Description should not be hidden for predicate download");
    assertNotNull(
        retrievedPredicateDownload4.getRequest().getCreator(), "Creator should not be hidden");
  }

  /** Tests the persistence of the DownloadRequest's DownloadFormat. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testDownloadFormatPersistence(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, eventDownloadResource, eventDownloadClient);
    Download eventDownload = getTestInstancePredicateDownload();
    DownloadFormat format = eventDownload.getRequest().getFormat();
    service.create(eventDownload);
    Download eventDownload2 = service.get(eventDownload.getKey());
    assertNotNull(eventDownload2);
    assertEquals(format, eventDownload2.getRequest().getFormat());
    assertEquals(eventDownload.getRequest(), eventDownload2.getRequest());
    assertEquals(eventDownload.getSource(), eventDownload2.getSource());
  }

  /** Creates a {@link Download} with a null status which should trigger a validation exception. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testCreateWithNullStatus(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, eventDownloadResource, eventDownloadClient);
    Download eventDownload = getTestInstancePredicateDownload();
    eventDownload.setStatus(null);
    assertThrows(ValidationException.class, () -> service.create(eventDownload));
  }

  /**
   * Creates several occurrence download with the same user name. And retrieves the downloads done
   * by the user.
   */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testList(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, eventDownloadResource, eventDownloadClient);
    // 3 PredicateDownloads
    for (int i = 1; i <= 3; i++) {
      service.create(getTestInstancePredicateDownload());
    }

    PagingResponse<Download> downloads = service.list(new PagingRequest(0, 20), null, null);
    int resultSize = downloads.getResults().size();
    long numberOfPredicateDownloads =
        downloads.getResults().stream()
            .filter(d -> d.getRequest() instanceof PredicateDownloadRequest)
            .count();
    // All numbers are compare to 2 different values because this each run twice: one for the WS and
    // once for the MyBatis layer
    assertEquals(3, resultSize, "A total of 3 records must be returned");
    assertEquals(resultSize, service.count(null, null));
    assertEquals(
        3L, numberOfPredicateDownloads, "A total of 3 PredicateDownloads must be returned");
  }

  /**
   * Creates several occurrence download with the same user name and attempts to get them with a
   * different user name.
   */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testListByUnauthorizedUser(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, eventDownloadResource, eventDownloadClient);
    // This test applies to web service calls only, requires a security context.
    if (getSimplePrincipalProvider() != null) {
      for (int i = 1; i <= 5; i++) {
        service.create(getTestInstancePredicateDownload());
      }
      assertFalse(
          service
              .listByUser(TEST_ADMIN, new PagingRequest(3, 5), null, null, true)
              .getResults()
              .isEmpty(),
          "List by user operation should return 5 records");

    } else {
      // Just to make the test pass for the webservice version
      throw new AccessControlException("Fake exception");
    }
  }

  /**
   * Creates several occurrence download with the same user name. And retrieves the downloads done
   * by the user.
   */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testListByUser(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, eventDownloadResource, eventDownloadClient);
    for (int i = 1; i <= 5; i++) {
      service.create(getTestInstancePredicateDownload());
    }
    assertFalse(
        service
            .listByUser(TEST_ADMIN, new PagingRequest(0, 5), null, null, true)
            .getResults()
            .isEmpty(),
        "List by user operation should return 5 records");
  }

  /**
   * Creates several occurrence download with running status and retrieves the downloads done by
   * status.
   */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testListByStatus(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, eventDownloadResource, eventDownloadClient);
    for (int i = 1; i <= 5; i++) {
      service.create(getTestInstancePredicateDownload());
    }
    assertFalse(
        service
            .list(new PagingRequest(0, 5), Status.EXECUTING_STATUSES, null)
            .getResults()
            .isEmpty(),
        "List by status operation should return 5 records");
  }

  /**
   * Creates several occurrence download with running status and retrieves the downloads done by
   * source.
   */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testListBySource(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, eventDownloadResource, eventDownloadClient);
    for (int i = 1; i <= 5; i++) {
      service.create(getTestInstancePredicateDownload());
    }
    assertEquals(
        5,
        service.list(new PagingRequest(0, 5), null, DEFAULT_SOURCE).getResults().size(),
        "List by source operation should return 5 records");

    assertEquals(
        0,
        service.list(new PagingRequest(0, 5), new HashSet<>(), "foo").getResults().size(),
        "List by source operation should return 0 records");
  }

  /**
   * Creates several occurrence download with running status and retrieves the downloads done by
   * source.
   */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testListBySourceAndStatus(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, eventDownloadResource, eventDownloadClient);
    for (int i = 1; i <= 5; i++) {
      service.create(getTestInstancePredicateDownload());
    }
    assertEquals(
        5,
        service
            .list(new PagingRequest(0, 5), Status.EXECUTING_STATUSES, DEFAULT_SOURCE)
            .getResults()
            .size(),
        "List by source and status operation should return 5 records");

    assertEquals(
        0,
        service.list(new PagingRequest(0, 5), Status.EXECUTING_STATUSES, "foo").getResults().size(),
        "List by source and status operation should return 0 records");

    assertEquals(
        0,
        service
            .list(new PagingRequest(0, 5), Collections.singleton(Status.CANCELLED), DEFAULT_SOURCE)
            .getResults()
            .size(),
        "List by source and status operation should return 0 records");
  }

  /**
   * Creates several occurrence download with the same user name. And retrieves the downloads done
   * by the user.
   */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testListByUserAndStatus(ServiceType serviceType) {
    LocalDateTime.now().atOffset(ZoneOffset.UTC).toString();

    OccurrenceDownloadService service =
        getService(serviceType, eventDownloadResource, eventDownloadClient);
    for (int i = 1; i <= 5; i++) {
      service.create(getTestInstancePredicateDownload());
    }

    PagingResponse<Download> downloads =
        service.listByUser(
            TEST_ADMIN,
            new PagingRequest(0, 5),
            Status.EXECUTING_STATUSES,
            LocalDateTime.now().minusMinutes(30),
            true);

    assertFalse(
        downloads.getResults().isEmpty(),
        "List by user and status operation should return 5 records");

    long count =
        service.countByUser(
            TEST_ADMIN, Status.EXECUTING_STATUSES, LocalDateTime.now().minusMinutes(30));
    assertEquals(downloads.getResults().size(), count);

    assertEquals(
        0,
        service
            .listByUser(
                TEST_ADMIN,
                new PagingRequest(0, 5),
                Status.EXECUTING_STATUSES,
                LocalDateTime.now(),
                true)
            .getResults()
            .size());

    assertEquals(
        0, service.countByUser(TEST_ADMIN, Status.EXECUTING_STATUSES, LocalDateTime.now()));
  }

  /** Tests the status update of {@link Download}. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testUpdateStatus(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, eventDownloadResource, eventDownloadClient);
    Download eventDownload = getTestInstancePredicateDownload();
    service.create(eventDownload);
    eventDownload.setStatus(Status.RUNNING);
    eventDownload.setSize(200L);
    eventDownload.setTotalRecords(600L);
    eventDownload.setDoi(new DOI("doi:10.1234/1ASCDU"));
    eventDownload.setLicense(License.CC0_1_0);
    eventDownload.setCreated(new Date());
    eventDownload.setModified(new Date());
    service.update(eventDownload);
    Download occurrenceDownload2 = service.get(eventDownload.getKey());
    assertSame(Status.RUNNING, occurrenceDownload2.getStatus());
    assertEquals(200, occurrenceDownload2.getSize());
    assertEquals(600, occurrenceDownload2.getTotalRecords());
  }

  /** Tests the status update of {@link Download}. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testUpdateStatusCompleted(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, eventDownloadResource, eventDownloadClient);
    Download eventDownload = getTestInstancePredicateDownload();
    service.create(eventDownload);
    // reload to get latest db modifications like created date
    eventDownload = service.get(eventDownload.getKey());

    eventDownload.setStatus(Status.SUCCEEDED);
    eventDownload.setSize(200L);
    eventDownload.setTotalRecords(600L);
    eventDownload.setDoi(new DOI("doi:10.1234/1ASCDU"));
    service.update(eventDownload);
    Download occurrenceDownload2 = service.get(eventDownload.getKey());
    assertSame(Status.SUCCEEDED, occurrenceDownload2.getStatus());
    assertNotNull(occurrenceDownload2.getModified());
    assertEquals(200L, occurrenceDownload2.getSize());
    assertEquals(600L, occurrenceDownload2.getTotalRecords());
  }

  /** Tests the status update of {@link Download}. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testUpdateStatusFailed(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, eventDownloadResource, eventDownloadClient);
    Download eventDownload = getTestInstancePredicateDownload();
    eventDownload.setStatus(Status.FAILED);
    service.create(eventDownload);
    // reload to get latest db modifications like created date
    eventDownload = service.get(eventDownload.getKey());
    eventDownload.setStatus(Status.FAILED);
    eventDownload.setSize(200L);
    eventDownload.setTotalRecords(600L);
    service.update(eventDownload);
    Download occurrenceDownload2 = service.get(eventDownload.getKey());
    assertSame(Status.FAILED, occurrenceDownload2.getStatus());
    assertNotNull(occurrenceDownload2.getModified());
    assertNull(occurrenceDownload2.getDoi());
    assertEquals(200L, occurrenceDownload2.getSize());
    assertEquals(600L, occurrenceDownload2.getTotalRecords());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testDownloadUsages(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, eventDownloadResource, eventDownloadClient);

    Download eventDownload = getTestInstancePredicateDownload();
    service.create(eventDownload);

    // endorsing node for the organization
    UUID nodeKey = nodeService.create(testDataFactory.newNode());

    // publishing organization (required field)
    Organization o = testDataFactory.newOrganization(nodeKey);
    o.setCountry(Country.DENMARK);
    UUID organizationKey = organizationService.create(o);

    Installation i = testDataFactory.newInstallation(organizationKey);
    UUID installationKey = installationService.create(i);
    Dataset dataset = testDataFactory.newDataset(organizationKey, installationKey);
    dataset.setKey(datasetService.create(dataset));

    Dataset dataset2 = testDataFactory.newDataset(organizationKey, installationKey);
    dataset2.setTitle("title2");
    dataset2.setKey(datasetService.create(dataset2));

    Organization o2 = testDataFactory.newOrganization(nodeKey);
    o2.setCountry(Country.SPAIN);
    UUID organization2Key = organizationService.create(o2);

    Dataset dataset3 = testDataFactory.newDataset(organization2Key, installationKey);
    dataset3.setTitle("title3");
    dataset3.setKey(datasetService.create(dataset3));

    Map<UUID, Long> usages = new HashMap<>();
    usages.put(dataset.getKey(), 1000L);
    usages.put(dataset2.getKey(), 500L);
    usages.put(dataset3.getKey(), 5300L);
    service.createUsages(eventDownload.getKey(), usages);

    PagingResponse<DatasetOccurrenceDownloadUsage> datasetUsages =
        service.listDatasetUsages(eventDownload.getKey(), null, null, null, new PagingRequest());
    assertEquals(3, datasetUsages.getResults().size());
    assertEquals(3, datasetUsages.getCount());
    datasetUsages =
        service.listDatasetUsages(
            eventDownload.getKey(), dataset.getTitle(), null, null, new PagingRequest());
    assertEquals(1, datasetUsages.getResults().size());
    datasetUsages =
        service.listDatasetUsages(
            eventDownload.getKey(),
            null,
            DatasetUsageSortField.RECORD_COUNT,
            SortOrder.DESC,
            new PagingRequest());
    assertEquals(dataset3.getKey(), datasetUsages.getResults().get(0).getDatasetKey());
    datasetUsages =
        service.listDatasetUsages(
            eventDownload.getKey(),
            null,
            DatasetUsageSortField.RECORD_COUNT,
            SortOrder.ASC,
            new PagingRequest());
    assertEquals(dataset2.getKey(), datasetUsages.getResults().get(0).getDatasetKey());

    PagingResponse<OrganizationOccurrenceDownloadUsage> organizationUsages =
        service.listOrganizationUsages(
            eventDownload.getKey(), null, null, null, new PagingRequest());
    assertEquals(2, organizationUsages.getResults().size());
    assertEquals(2, organizationUsages.getCount());

    organizationUsages =
        service.listOrganizationUsages(
            eventDownload.getKey(),
            null,
            OrganizationUsageSortField.COUNTRY_CODE,
            null,
            new PagingRequest(0, 1));
    assertEquals(1, organizationUsages.getResults().size());
    assertEquals(2, organizationUsages.getCount());

    organizationUsages =
        service.listOrganizationUsages(
            eventDownload.getKey(),
            null,
            OrganizationUsageSortField.COUNTRY_CODE,
            SortOrder.DESC,
            new PagingRequest(1, 1));
    assertEquals(1, organizationUsages.getResults().size());
    assertEquals(2, organizationUsages.getCount());
    assertEquals(o.getKey(), organizationUsages.getResults().get(0).getOrganizationKey());

    organizationUsages =
        service.listOrganizationUsages(
            eventDownload.getKey(),
            null,
            OrganizationUsageSortField.COUNTRY_CODE,
            null,
            new PagingRequest());
    assertEquals(o.getKey(), organizationUsages.getResults().get(0).getOrganizationKey());
    organizationUsages =
        service.listOrganizationUsages(
            eventDownload.getKey(),
            null,
            OrganizationUsageSortField.RECORD_COUNT,
            SortOrder.DESC,
            new PagingRequest());
    assertEquals(o2.getKey(), organizationUsages.getResults().get(0).getOrganizationKey());

    organizationUsages =
        service.listOrganizationUsages(
            eventDownload.getKey(),
            null,
            OrganizationUsageSortField.RECORD_COUNT,
            SortOrder.ASC,
            new PagingRequest());
    assertEquals(o.getKey(), organizationUsages.getResults().get(0).getOrganizationKey());

    PagingResponse<CountryOccurrenceDownloadUsage> countryUsages =
        service.listCountryUsages(eventDownload.getKey(), null, null, new PagingRequest());
    assertEquals(2, countryUsages.getResults().size());
    assertEquals(2, countryUsages.getCount());

    countryUsages =
        service.listCountryUsages(
            eventDownload.getKey(),
            CountryUsageSortField.COUNTRY_CODE,
            SortOrder.DESC,
            new PagingRequest(1, 1));
    assertEquals(1, countryUsages.getResults().size());
    assertEquals(2, countryUsages.getCount());
    assertEquals(
        Country.DENMARK.getIso2LetterCode(),
        countryUsages.getResults().get(0).getPublishingCountryCode());

    countryUsages =
        service.listCountryUsages(
            eventDownload.getKey(),
            CountryUsageSortField.COUNTRY_CODE,
            SortOrder.DESC,
            new PagingRequest());
    assertEquals(
        Country.SPAIN.getIso2LetterCode(),
        countryUsages.getResults().get(0).getPublishingCountryCode());
    countryUsages =
        service.listCountryUsages(
            eventDownload.getKey(), CountryUsageSortField.RECORD_COUNT, null, new PagingRequest());
    assertEquals(
        Country.DENMARK.getIso2LetterCode(),
        countryUsages.getResults().get(0).getPublishingCountryCode());
  }
}
