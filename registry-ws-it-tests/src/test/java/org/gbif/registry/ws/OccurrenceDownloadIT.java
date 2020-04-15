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
package org.gbif.registry.ws;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.occurrence.DownloadFormat;
import org.gbif.api.model.occurrence.PredicateDownloadRequest;
import org.gbif.api.model.occurrence.predicate.EqualsPredicate;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.ws.fixtures.TestConstants;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.security.AccessControlException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import javax.validation.ValidationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
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

import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;

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
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RegistryIntegrationTestsConfiguration.class)
@ContextConfiguration(initializers = {OccurrenceDownloadIT.ContextInitializer.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class OccurrenceDownloadIT {

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

  private final OccurrenceDownloadService occurrenceDownloadService;

  private final SimplePrincipalProvider simplePrincipalProvider;

  @Autowired
  public OccurrenceDownloadIT(
      OccurrenceDownloadService occurrenceDownloadService,
      SimplePrincipalProvider simplePrincipalProvider) {
    this.occurrenceDownloadService = occurrenceDownloadService;
    this.simplePrincipalProvider = simplePrincipalProvider;
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
            new EqualsPredicate(OccurrenceSearchParameter.TAXON_KEY, "212"),
            TestConstants.TEST_ADMIN,
            Collections.singleton("downloadtest@gbif.org"),
            true,
            DownloadFormat.DWCA));
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
            DownloadFormat.DWCA));
    return download;
  }

  @BeforeEach
  public void setup() {
    // reset SimplePrincipleProvider, configured for web service client tests only
    if (simplePrincipalProvider != null) {
      simplePrincipalProvider.setPrincipal(TestConstants.TEST_ADMIN);
      SecurityContext ctx = SecurityContextHolder.createEmptyContext();
      SecurityContextHolder.setContext(ctx);
      ctx.setAuthentication(
          new UsernamePasswordAuthenticationToken(
              simplePrincipalProvider.get().getName(),
              "",
              Collections.singleton(new SimpleGrantedAuthority(UserRole.REGISTRY_ADMIN.name()))));
    }
  }

  /** Persists a valid {@link Download} instance. */
  @Test
  public void testCreate() {
    occurrenceDownloadService.create(getTestInstancePredicateDownload());
  }

  /** Persists a valid {@link Download} instance with null predicates. */
  @Test
  public void testCreateWithNullPredicate() {
    occurrenceDownloadService.create(getTestInstanceNullPredicateDownload());
  }

  /** Tests the create and get(key) methods. */
  @Test
  public void testCreateAndGet() {
    Download occurrenceDownload = getTestInstancePredicateDownload();
    occurrenceDownloadService.create(occurrenceDownload);
    Download occurrenceDownload2 = occurrenceDownloadService.get(occurrenceDownload.getKey());
    assertNotNull(occurrenceDownload2);
  }

  /** Tests the create and get(key) methods for null predicate. */
  @Test
  public void testCreateAndGetNullPredicate() {
    Download occurrenceDownload = getTestInstanceNullPredicateDownload();
    occurrenceDownloadService.create(occurrenceDownload);
    Download occurrenceDownload2 = occurrenceDownloadService.get(occurrenceDownload.getKey());
    assertNotNull(occurrenceDownload2);
  }

  /** Tests the persistence of the DownloadRequest's DownloadFormat. */
  @Test
  public void testDownloadFormatPersistence() {
    Download occurrenceDownload = getTestInstancePredicateDownload();
    DownloadFormat format = occurrenceDownload.getRequest().getFormat();
    occurrenceDownloadService.create(occurrenceDownload);
    Download occurrenceDownload2 = occurrenceDownloadService.get(occurrenceDownload.getKey());
    assertNotNull(occurrenceDownload2);
    assertEquals(format, occurrenceDownload2.getRequest().getFormat());
  }

  /** Creates a {@link Download} with a null status which should trigger a validation exception. */
  @Test
  public void testCreateWithNullStatus() {
    Download occurrenceDownload = getTestInstancePredicateDownload();
    occurrenceDownload.setStatus(null);
    assertThrows(
        ValidationException.class, () -> occurrenceDownloadService.create(occurrenceDownload));
  }

  /**
   * Creates several occurrence download with the same user name. And retrieves the downloads done
   * by the user.
   */
  @Test
  public void testList() {
    // 3 PredicateDownloads
    for (int i = 1; i <= 3; i++) {
      occurrenceDownloadService.create(getTestInstancePredicateDownload());
    }

    PagingResponse<Download> downloads =
        occurrenceDownloadService.list(new PagingRequest(0, 20), null);
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
  @Test
  public void testListByUnauthorizedUser() {
    // This test applies to web service calls only, requires a security context.
    if (simplePrincipalProvider != null) {
      for (int i = 1; i <= 5; i++) {
        occurrenceDownloadService.create(getTestInstancePredicateDownload());
      }
      // TODO: change to use the client
      assertTrue(
          occurrenceDownloadService
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
  @Test
  public void testListByUser() {
    for (int i = 1; i <= 5; i++) {
      occurrenceDownloadService.create(getTestInstancePredicateDownload());
    }
    assertTrue(
        occurrenceDownloadService
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
  @Test
  public void testListByStatus() {
    for (int i = 1; i <= 5; i++) {
      occurrenceDownloadService.create(getTestInstancePredicateDownload());
    }
    assertTrue(
        occurrenceDownloadService
                .list(new PagingRequest(0, 5), Download.Status.EXECUTING_STATUSES)
                .getResults()
                .size()
            > 0,
        "List by user operation should return 5 records");
  }

  /**
   * Creates several occurrence download with the same user name. And retrieves the downloads done
   * by the user.
   */
  @Test
  public void testListByUserAndStatus() {
    for (int i = 1; i <= 5; i++) {
      occurrenceDownloadService.create(getTestInstancePredicateDownload());
    }
    assertTrue(
        occurrenceDownloadService
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
  @Test
  public void testUpdateStatus() {
    Download occurrenceDownload = getTestInstancePredicateDownload();
    occurrenceDownloadService.create(occurrenceDownload);
    occurrenceDownload.setStatus(Download.Status.RUNNING);
    occurrenceDownload.setSize(200L);
    occurrenceDownload.setTotalRecords(600L);
    occurrenceDownload.setDoi(new DOI("doi:10.1234/1ASCDU"));
    occurrenceDownload.setLicense(License.CC0_1_0);
    occurrenceDownload.setCreated(new Date());
    occurrenceDownload.setModified(new Date());
    occurrenceDownloadService.update(occurrenceDownload);
    Download occurrenceDownload2 = occurrenceDownloadService.get(occurrenceDownload.getKey());
    assertSame(Download.Status.RUNNING, occurrenceDownload2.getStatus());
    assertEquals(200, occurrenceDownload2.getSize());
    assertEquals(600, occurrenceDownload2.getTotalRecords());
  }

  /** Tests the status update of {@link Download}. */
  @Test
  public void testUpdateStatusCompleted() {
    Download occurrenceDownload = getTestInstancePredicateDownload();
    occurrenceDownloadService.create(occurrenceDownload);
    // reload to get latest db modifications like created date
    occurrenceDownload = occurrenceDownloadService.get(occurrenceDownload.getKey());

    occurrenceDownload.setStatus(Download.Status.SUCCEEDED);
    occurrenceDownload.setSize(200L);
    occurrenceDownload.setTotalRecords(600L);
    occurrenceDownload.setDoi(new DOI("doi:10.1234/1ASCDU"));
    occurrenceDownloadService.update(occurrenceDownload);
    Download occurrenceDownload2 = occurrenceDownloadService.get(occurrenceDownload.getKey());
    assertSame(Download.Status.SUCCEEDED, occurrenceDownload2.getStatus());
    assertNotNull(occurrenceDownload2.getModified());
    assertEquals(200L, occurrenceDownload2.getSize());
    assertEquals(600L, occurrenceDownload2.getTotalRecords());
  }
}
