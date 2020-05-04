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
package org.gbif.registry.ws.it;

import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.fixtures.TestConstants;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import io.zonky.test.db.postgres.junit5.PreparedDbExtension;

/** Base class for IT tests that initializes data sources and basic security settings. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = RegistryIntegrationTestsConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {BaseItTest.ContextInitializer.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext
public class BaseItTest {

  public static final String DB_LOCK = "db_lock";

  /** Custom ContextInitializer to expose the registry DB data source and search flags. */
  public static class ContextInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of(
              Stream.of(dbTestPropertyPairs()).flatMap(Stream::of).toArray(String[]::new))
          .applyTo(configurableApplicationContext.getEnvironment());
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

  @RegisterExtension
  public static PreparedDbExtension database =
      EmbeddedPostgresExtension.preparedDatabase(
          LiquibasePreparer.forClasspathLocation(TestConstants.LIQUIBASE_MASTER_FILE));

  @RegisterExtension
  public final DatabaseInitializer databaseRule =
      new DatabaseInitializer(database.getTestDatabase());

  private final SimplePrincipalProvider simplePrincipalProvider;
  protected static EsManageServer esServer;

  public BaseItTest(SimplePrincipalProvider simplePrincipalProvider, EsManageServer esServer) {
    this.simplePrincipalProvider = simplePrincipalProvider;
    BaseItTest.esServer = esServer;
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

  public SimplePrincipalProvider getSimplePrincipalProvider() {
    return simplePrincipalProvider;
  }

  public enum ServiceType {
    RESOURCE,
    CLIENT
  }
}
