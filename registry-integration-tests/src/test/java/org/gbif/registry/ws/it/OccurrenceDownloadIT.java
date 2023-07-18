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
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.occurrence.DownloadFormat;
import org.gbif.api.model.occurrence.DownloadType;
import org.gbif.api.model.occurrence.PredicateDownloadRequest;
import org.gbif.api.model.occurrence.predicate.EqualsPredicate;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
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
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.client.OccurrenceDownloadClient;
import org.gbif.registry.ws.it.fixtures.TestConstants;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.security.AccessControlException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import javax.validation.ValidationException;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs tests for the {@link OccurrenceDownloadService} implementations. This is parameterized to
 * run the same test routines for the following:
 *
 * <ol>
 *   <li>The persistence layer
 *   <li>The WS service layer
 *   <li>The WS service client layer
 * </ol>
 */
public class OccurrenceDownloadIT extends BaseItTest {

  private static final String DEFAULT_SOURCE = "testSource";

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer();

  private TestDataFactory testDataFactory;
  private final OccurrenceDownloadService occurrenceDownloadResource;
  private final OccurrenceDownloadClient occurrenceDownloadClient;

  // The following services are required to create dataset instances
  private final DatasetService datasetService;
  private final OrganizationService organizationService;
  private final NodeService nodeService;
  private final InstallationService installationService;

  @Autowired
  public OccurrenceDownloadIT(
      OccurrenceDownloadService occurrenceDownloadResource,
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
    this.occurrenceDownloadResource = occurrenceDownloadResource;
    this.organizationService = organizationService;
    this.datasetService = datasetService;
    this.nodeService = nodeService;
    this.installationService = installationService;
    this.occurrenceDownloadClient =
        prepareClient(localServerPort, keyStore, OccurrenceDownloadClient.class);
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
    download.setStatus(Download.Status.PREPARING);
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
            new EqualsPredicate(OccurrenceSearchParameter.TAXON_KEY, "212", false),
            TestConstants.TEST_ADMIN,
            Collections.singleton("downloadtest@gbif.org"),
            true,
            DownloadFormat.DWCA,
            DownloadType.OCCURRENCE,
            Collections.singleton(Extension.AUDUBON)));
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
            TestConstants.TEST_ADMIN,
            Collections.singleton("downloadtest@gbif.org"),
            true,
            DownloadFormat.DWCA,
            DownloadType.OCCURRENCE,
            Collections.singleton(Extension.AUDUBON)));
    return download;
  }

  /** Persists a valid {@link Download} instance. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testCreate(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, occurrenceDownloadResource, occurrenceDownloadClient);
    service.create(getTestInstancePredicateDownload());
  }

  /** Persists a valid {@link Download} instance with null predicates. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testCreateWithNullPredicate(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, occurrenceDownloadResource, occurrenceDownloadClient);
    service.create(getTestInstanceNullPredicateDownload());
  }

  /** Tests the create and get(key) methods. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testCreateAndGet(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, occurrenceDownloadResource, occurrenceDownloadClient);
    Download occurrenceDownload = getTestInstancePredicateDownload();
    service.create(occurrenceDownload);
    Download occurrenceDownload2 = service.get(occurrenceDownload.getKey());
    assertNotNull(occurrenceDownload2);
    assertEquals(occurrenceDownload.getRequest(), occurrenceDownload2.getRequest());
  }

  /** Tests the create and get(key) methods for null predicate. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testCreateAndGetNullPredicate(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, occurrenceDownloadResource, occurrenceDownloadClient);
    Download occurrenceDownload = getTestInstanceNullPredicateDownload();
    service.create(occurrenceDownload);
    Download occurrenceDownload2 = service.get(occurrenceDownload.getKey());
    assertNotNull(occurrenceDownload2);
    assertEquals(occurrenceDownload.getRequest(), occurrenceDownload2.getRequest());
  }

  /** Tests the persistence of the DownloadRequest's DownloadFormat. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testDownloadFormatPersistence(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, occurrenceDownloadResource, occurrenceDownloadClient);
    Download occurrenceDownload = getTestInstancePredicateDownload();
    DownloadFormat format = occurrenceDownload.getRequest().getFormat();
    service.create(occurrenceDownload);
    Download occurrenceDownload2 = service.get(occurrenceDownload.getKey());
    assertNotNull(occurrenceDownload2);
    assertEquals(format, occurrenceDownload2.getRequest().getFormat());
    assertEquals(occurrenceDownload.getRequest(), occurrenceDownload2.getRequest());
    assertEquals(occurrenceDownload.getSource(), occurrenceDownload2.getSource());
  }

  /** Creates a {@link Download} with a null status which should trigger a validation exception. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testCreateWithNullStatus(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, occurrenceDownloadResource, occurrenceDownloadClient);
    Download occurrenceDownload = getTestInstancePredicateDownload();
    occurrenceDownload.setStatus(null);
    assertThrows(ValidationException.class, () -> service.create(occurrenceDownload));
  }

  /**
   * Creates several occurrence download with the same user name. And retrieves the downloads done
   * by the user.
   */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testList(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, occurrenceDownloadResource, occurrenceDownloadClient);
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
        getService(serviceType, occurrenceDownloadResource, occurrenceDownloadClient);
    // This test applies to web service calls only, requires a security context.
    if (getSimplePrincipalProvider() != null) {
      for (int i = 1; i <= 5; i++) {
        service.create(getTestInstancePredicateDownload());
      }
      assertTrue(
          service
                  .listByUser(TestConstants.TEST_ADMIN, new PagingRequest(3, 5), null)
                  .getResults()
                  .size()
              > 0,
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
        getService(serviceType, occurrenceDownloadResource, occurrenceDownloadClient);
    for (int i = 1; i <= 5; i++) {
      service.create(getTestInstancePredicateDownload());
    }
    assertTrue(
        service
                .listByUser(TestConstants.TEST_ADMIN, new PagingRequest(3, 5), null)
                .getResults()
                .size()
            > 0,
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
        getService(serviceType, occurrenceDownloadResource, occurrenceDownloadClient);
    for (int i = 1; i <= 5; i++) {
      service.create(getTestInstancePredicateDownload());
    }
    assertTrue(
        service
                .list(new PagingRequest(0, 5), Download.Status.EXECUTING_STATUSES, null)
                .getResults()
                .size()
            > 0,
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
        getService(serviceType, occurrenceDownloadResource, occurrenceDownloadClient);
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
        getService(serviceType, occurrenceDownloadResource, occurrenceDownloadClient);
    for (int i = 1; i <= 5; i++) {
      service.create(getTestInstancePredicateDownload());
    }
    assertEquals(
        5,
        service
            .list(new PagingRequest(0, 5), Download.Status.EXECUTING_STATUSES, DEFAULT_SOURCE)
            .getResults()
            .size(),
        "List by source and status operation should return 5 records");

    assertEquals(
        0,
        service
            .list(new PagingRequest(0, 5), Download.Status.EXECUTING_STATUSES, "foo")
            .getResults()
            .size(),
        "List by source and status operation should return 0 records");

    assertEquals(
        0,
        service
            .list(
                new PagingRequest(0, 5),
                Collections.singleton(Download.Status.CANCELLED),
                DEFAULT_SOURCE)
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
    OccurrenceDownloadService service =
        getService(serviceType, occurrenceDownloadResource, occurrenceDownloadClient);
    for (int i = 1; i <= 5; i++) {
      service.create(getTestInstancePredicateDownload());
    }
    assertTrue(
        service
                .listByUser(
                    TestConstants.TEST_ADMIN,
                    new PagingRequest(0, 5),
                    Download.Status.EXECUTING_STATUSES)
                .getResults()
                .size()
            > 0,
        "List by user and status operation should return 5 records");
  }

  /** Tests the status update of {@link Download}. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testUpdateStatus(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, occurrenceDownloadResource, occurrenceDownloadClient);
    Download occurrenceDownload = getTestInstancePredicateDownload();
    service.create(occurrenceDownload);
    occurrenceDownload.setStatus(Download.Status.RUNNING);
    occurrenceDownload.setSize(200L);
    occurrenceDownload.setTotalRecords(600L);
    occurrenceDownload.setDoi(new DOI("doi:10.1234/1ASCDU"));
    occurrenceDownload.setLicense(License.CC0_1_0);
    occurrenceDownload.setCreated(new Date());
    occurrenceDownload.setModified(new Date());
    service.update(occurrenceDownload);
    Download occurrenceDownload2 = service.get(occurrenceDownload.getKey());
    assertSame(Download.Status.RUNNING, occurrenceDownload2.getStatus());
    assertEquals(200, occurrenceDownload2.getSize());
    assertEquals(600, occurrenceDownload2.getTotalRecords());
  }

  /** Tests the status update of {@link Download}. */
  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testUpdateStatusCompleted(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, occurrenceDownloadResource, occurrenceDownloadClient);
    Download occurrenceDownload = getTestInstancePredicateDownload();
    service.create(occurrenceDownload);
    // reload to get latest db modifications like created date
    occurrenceDownload = service.get(occurrenceDownload.getKey());

    occurrenceDownload.setStatus(Download.Status.SUCCEEDED);
    occurrenceDownload.setSize(200L);
    occurrenceDownload.setTotalRecords(600L);
    occurrenceDownload.setDoi(new DOI("doi:10.1234/1ASCDU"));
    service.update(occurrenceDownload);
    Download occurrenceDownload2 = service.get(occurrenceDownload.getKey());
    assertSame(Download.Status.SUCCEEDED, occurrenceDownload2.getStatus());
    assertNotNull(occurrenceDownload2.getModified());
    assertEquals(200L, occurrenceDownload2.getSize());
    assertEquals(600L, occurrenceDownload2.getTotalRecords());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testDownloadUsages(ServiceType serviceType) {
    OccurrenceDownloadService service =
        getService(serviceType, occurrenceDownloadResource, occurrenceDownloadClient);

    Download occurrenceDownload = getTestInstancePredicateDownload();
    service.create(occurrenceDownload);

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
    service.createUsages(occurrenceDownload.getKey(), usages);

    PagingResponse<DatasetOccurrenceDownloadUsage> datasetUsages =
        service.listDatasetUsages(
            occurrenceDownload.getKey(), null, null, null, new PagingRequest());
    assertEquals(3, datasetUsages.getResults().size());
    assertEquals(3, datasetUsages.getCount());
    datasetUsages =
        service.listDatasetUsages(
            occurrenceDownload.getKey(), dataset.getTitle(), null, null, new PagingRequest());
    assertEquals(1, datasetUsages.getResults().size());
    datasetUsages =
        service.listDatasetUsages(
            occurrenceDownload.getKey(),
            null,
            DatasetUsageSortField.RECORD_COUNT,
            SortOrder.DESC,
            new PagingRequest());
    assertEquals(dataset3.getKey(), datasetUsages.getResults().get(0).getDatasetKey());
    datasetUsages =
        service.listDatasetUsages(
            occurrenceDownload.getKey(),
            null,
            DatasetUsageSortField.RECORD_COUNT,
            SortOrder.ASC,
            new PagingRequest());
    assertEquals(dataset2.getKey(), datasetUsages.getResults().get(0).getDatasetKey());

    PagingResponse<OrganizationOccurrenceDownloadUsage> organizationUsages =
        service.listOrganizationUsages(
            occurrenceDownload.getKey(), null, null, null, new PagingRequest());
    assertEquals(2, organizationUsages.getResults().size());
    assertEquals(2, organizationUsages.getCount());
    organizationUsages =
        service.listOrganizationUsages(
            occurrenceDownload.getKey(),
            null,
            OrganizationUsageSortField.COUNTRY_CODE,
            null,
            new PagingRequest());
    assertEquals(o.getKey(), organizationUsages.getResults().get(0).getOrganizationKey());
    organizationUsages =
        service.listOrganizationUsages(
            occurrenceDownload.getKey(),
            null,
            OrganizationUsageSortField.RECORD_COUNT,
            SortOrder.DESC,
            new PagingRequest());
    assertEquals(o2.getKey(), organizationUsages.getResults().get(0).getOrganizationKey());

    organizationUsages =
        service.listOrganizationUsages(
            occurrenceDownload.getKey(),
            null,
            OrganizationUsageSortField.RECORD_COUNT,
            SortOrder.ASC,
            new PagingRequest());
    assertEquals(o.getKey(), organizationUsages.getResults().get(0).getOrganizationKey());

    PagingResponse<CountryOccurrenceDownloadUsage> countryUsages =
        service.listCountryUsages(occurrenceDownload.getKey(), null, null, new PagingRequest());
    assertEquals(2, countryUsages.getResults().size());
    assertEquals(2, countryUsages.getCount());

    countryUsages =
        service.listCountryUsages(
            occurrenceDownload.getKey(),
            CountryUsageSortField.COUNTRY_CODE,
            SortOrder.DESC,
            new PagingRequest());
    assertEquals(
        Country.SPAIN.getIso2LetterCode(),
        countryUsages.getResults().get(0).getPublishingCountryCode());
    countryUsages =
        service.listCountryUsages(
            occurrenceDownload.getKey(),
            CountryUsageSortField.RECORD_COUNT,
            null,
            new PagingRequest());
    assertEquals(
        Country.DENMARK.getIso2LetterCode(),
        countryUsages.getResults().get(0).getPublishingCountryCode());
  }
}
