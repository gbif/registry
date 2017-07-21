/*
 * Copyright 2013 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry;

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
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.utils.Installations;
import org.gbif.registry.utils.Nodes;
import org.gbif.registry.utils.Organizations;
import org.gbif.registry.ws.resources.InstallationResource;
import org.gbif.registry.ws.resources.NodeResource;
import org.gbif.registry.ws.resources.OrganizationResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Date;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.gbif.registry.guice.RegistryTestModules.webservice;
import static org.gbif.registry.guice.RegistryTestModules.webserviceClient;

/**
 * Runs tests for the {@link MetasyncHistoryService} implementations.
 * This is parameterized to run the same test routines for the following:
 * <ol>
 * <li>The WS service layer</li>
 * <li>The WS service client layer</li>
 * </ol>
 */
@RunWith(Parameterized.class)
public class MetasyncHistoryIT {

  // Flushes the database on each run
  @ClassRule
  public static final LiquibaseInitializer liquibaseRule = new LiquibaseInitializer(LiquibaseModules.database());

  @ClassRule
  public static final RegistryServer registryServer = RegistryServer.INSTANCE;

  // Tests user
  private static String TEST_USER = "admin";

  @Rule
  public final DatabaseInitializer databaseRule = new DatabaseInitializer(LiquibaseModules.database());

  private final MetasyncHistoryService metasyncHistoryService;

  private final SimplePrincipalProvider simplePrincipalProvider;

  private final OrganizationService organizationService;
  private final NodeService nodeService;
  private final InstallationService installationService;

  public MetasyncHistoryIT(
    MetasyncHistoryService metasyncHistoryService,
    OrganizationService organizationService, NodeService nodeService,
    InstallationService installationService,
    SimplePrincipalProvider simplePrincipalProvider) {
    this.metasyncHistoryService = metasyncHistoryService;
    this.organizationService = organizationService;
    this.nodeService = nodeService;
    this.installationService = installationService;
    this.simplePrincipalProvider = simplePrincipalProvider;
  }

  @Parameters
  public static Iterable<Object[]> data() {
    final Injector webservice = webservice();
    final Injector client = webserviceClient();
    return ImmutableList.<Object[]>of(new Object[] {webservice.getInstance(InstallationResource.class),
      webservice.getInstance(OrganizationResource.class), webservice.getInstance(NodeResource.class),
      webservice.getInstance(InstallationResource.class), null},
      new Object[] {client.getInstance(MetasyncHistoryService.class),
        client.getInstance(OrganizationService.class), client.getInstance(NodeService.class),
        client.getInstance(InstallationService.class), client.getInstance(SimplePrincipalProvider.class)});
  }


  @Before
  public void setup() {
    // reset SimplePrincipleProvider, configured for web service client tests only
    if (simplePrincipalProvider != null) {
      simplePrincipalProvider.setPrincipal(TEST_USER);
    }
  }

  /**
   * Tests the operations create and list of {@link MetasyncHistoryService}.
   */
  @Test
  public void testCreateAndList() {
    MetasyncHistory metasyncHistory = getTestInstance();
    metasyncHistoryService.createMetasync(metasyncHistory);
    PagingResponse<MetasyncHistory> response =
      metasyncHistoryService.listMetasync(new PagingRequest());
    Assert.assertTrue("The list operation should return at least 1 record", response.getResults().size() > 0);
  }


  /**
   * Tests the {@link MetasyncHistoryService#listByInstallation(UUID, org.gbif.api.model.common.paging.Pageable)}
   * operation.
   */
  @Test
  public void testListAndListByInstallation() {
    MetasyncHistory metasyncHistory = getTestInstance();
    metasyncHistoryService.createMetasync(metasyncHistory);
    PagingResponse<MetasyncHistory> response =
      metasyncHistoryService.listMetasync(metasyncHistory.getInstallationKey(), new PagingRequest());
    Assert.assertTrue("The list operation should return at least 1 record", response.getResults().size() > 0);
  }

  /**
   * Creates a test installation. The installation is persisted in the data base.
   * The organization related to the installation are created too.
   */
  private Installation createTestInstallation() {
    // endorsing node for the organization
    UUID nodeKey = nodeService.create(Nodes.newInstance());

    // publishing organization (required field)
    Organization org = Organizations.newInstance(nodeKey);
    UUID organizationKey = organizationService.create(org);

    Installation inst = Installations.newInstance(organizationKey);
    UUID installationKey = installationService.create(inst);
    inst.setKey(installationKey);
    return inst;
  }

  /**
   * Creates {@link MetasyncHistory} object to be used in test cases.
   */
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
