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

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.metasync.MetasyncHistory;
import org.gbif.api.model.registry.metasync.MetasyncResult;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.MetasyncHistoryService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.fixtures.TestConstants;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Collections;
import java.util.Date;
import java.util.UUID;

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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs tests for the {@link MetasyncHistoryService} implementations. This is parameterized to run
 * the same test routines for the following:
 *
 * <ol>
 *   <li>The WS service layer
 *   <li>The WS service client layer
 * </ol>
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RegistryIntegrationTestsConfiguration.class)
@ContextConfiguration(initializers = {MetasyncHistoryIT.ContextInitializer.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class MetasyncHistoryIT {

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

  private final MetasyncHistoryService metasyncHistoryService;
  private final SimplePrincipalProvider simplePrincipalProvider;

  private final OrganizationService organizationService;
  private final NodeService nodeService;
  private final InstallationService installationService;

  private final TestDataFactory testDataFactory;

  @Autowired
  public MetasyncHistoryIT(
      MetasyncHistoryService metasyncHistoryService,
      OrganizationService organizationService,
      NodeService nodeService,
      InstallationService installationService,
      SimplePrincipalProvider simplePrincipalProvider,
      TestDataFactory testDataFactory) {
    this.metasyncHistoryService = metasyncHistoryService;
    this.organizationService = organizationService;
    this.nodeService = nodeService;
    this.installationService = installationService;
    this.simplePrincipalProvider = simplePrincipalProvider;
    this.testDataFactory = testDataFactory;
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

  /** Tests the operations create and list of {@link MetasyncHistoryService}. */
  @Test
  public void testCreateAndList() {
    MetasyncHistory metasyncHistory = getTestInstance();
    metasyncHistoryService.createMetasync(metasyncHistory);
    PagingResponse<MetasyncHistory> response =
        metasyncHistoryService.listMetasync(new PagingRequest());
    assertTrue(
        response.getResults().size() > 0, "The list operation should return at least 1 record");
  }

  /** Tests the {@link MetasyncHistoryService#listMetasync(UUID, Pageable)} operation. */
  @Test
  public void testListAndListByInstallation() {
    MetasyncHistory metasyncHistory = getTestInstance();
    metasyncHistoryService.createMetasync(metasyncHistory);
    PagingResponse<MetasyncHistory> response =
        metasyncHistoryService.listMetasync(
            metasyncHistory.getInstallationKey(), new PagingRequest());
    assertTrue(
        response.getResults().size() > 0, "The list operation should return at least 1 record");
  }

  /**
   * Creates a test installation. The installation is persisted in the data base. The organization
   * related to the installation are created too.
   */
  private Installation createTestInstallation() {
    // endorsing node for the organization
    UUID nodeKey = nodeService.create(testDataFactory.newNode());

    // publishing organization (required field)
    Organization org = testDataFactory.newOrganization(nodeKey);
    UUID organizationKey = organizationService.create(org);

    Installation inst = testDataFactory.newInstallation(organizationKey);
    UUID installationKey = installationService.create(inst);
    inst.setKey(installationKey);
    return inst;
  }

  /** Creates {@link MetasyncHistory} object to be used in test cases. */
  private MetasyncHistory getTestInstance() {
    MetasyncHistory metasyncHistory = new MetasyncHistory();
    metasyncHistory.setDetails("testDetails");
    metasyncHistory.setResult(MetasyncResult.OK);
    metasyncHistory.setSyncDate(new Date());
    Installation installation = createTestInstallation();
    metasyncHistory.setInstallationKey(installation.getKey());
    return metasyncHistory;
  }
}
