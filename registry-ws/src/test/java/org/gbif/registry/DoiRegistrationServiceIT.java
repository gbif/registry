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

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.doi.DoiType;
import org.gbif.registry.doi.registration.DoiRegistration;
import org.gbif.registry.doi.registration.DoiRegistrationService;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.ws.resources.DoiRegistrationResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.gbif.registry.guice.RegistryTestModules.webservice;
import static org.gbif.registry.guice.RegistryTestModules.webserviceClient;

import static org.junit.Assert.assertNotNull;

/**
 * This is parameterized to run the same test routines for the following DOI Registration elements:
 * <ol>
 * <li>The persistence layer</li>
 * <li>The WS service layer</li>
 * <li>The WS service client layer</li>
 * </ol>
 */
@RunWith(Parameterized.class)
public class DoiRegistrationServiceIT {

  // Flushes the database on each run
  @ClassRule
  public static final LiquibaseInitializer liquibaseRule = new LiquibaseInitializer(RegistryTestModules.database());

  @ClassRule
  public static final RegistryServer registryServer = RegistryServer.INSTANCE;

  // Tests user
  private static String TEST_ADMIN_USER = "admin";

  @Rule
  public final DatabaseInitializer databaseRule = new DatabaseInitializer(RegistryTestModules.database());

  private final DoiRegistrationService doiRegistrationService;

  private final SimplePrincipalProvider simplePrincipalProvider;

  public DoiRegistrationServiceIT(
    DoiRegistrationService doiRegistrationService,
    SimplePrincipalProvider simplePrincipalProvider) {
    this.doiRegistrationService = doiRegistrationService;
    this.simplePrincipalProvider = simplePrincipalProvider;
  }

  @Parameters
  public static Iterable<Object[]> data() {
    final Injector webservice = webservice();
    final Injector client = webserviceClient();

    return ImmutableList.of(
      new Object[] {webservice.getInstance(DoiRegistrationResource.class), null},
      new Object[] {client.getInstance(DoiRegistrationService.class),
        client.getInstance(SimplePrincipalProvider.class)});
  }

  @Before
  public void setup() {
    setPrincipal(TEST_ADMIN_USER);
  }


  /**
   * Generates a new DOI.
   */
  @Test
  public void testGenerate() {
    DOI doi = doiRegistrationService.generate(DoiType.DATA_PACKAGE);
    assertNotNull(doi);
  }

  /**
   * Tests the generate and get DOI methods.
   */
  @Test
  public void testCreateAndGet() {
    DOI doi = doiRegistrationService.generate(DoiType.DATA_PACKAGE);
    DoiData doiData = doiRegistrationService.get(doi.getPrefix(), doi.getSuffix());
    assertNotNull(doi);
    assertNotNull(doiData);
  }

  /**
   * Tests the registration of DOI for a data package.
   */
  @Test
  public void testRegister() {
    DoiRegistration doiRegistration = new DoiRegistration();
    doiRegistration.setType(DoiType.DATA_PACKAGE);
    doiRegistration.setUser(TEST_ADMIN_USER);
    doiRegistration.setMetadata(DataCiteMetadata.builder().build());
    DOI doi = doiRegistrationService.register(doiRegistration);
    assertNotNull(doi);
  }

  /**
   * Sets the user name as current authenticated user.
   */
  private void setPrincipal(String username) {
    // reset SimplePrincipleProvider, configured for web service client tests only
    if (simplePrincipalProvider != null) {
      simplePrincipalProvider.setPrincipal(username);
    }
  }

}
