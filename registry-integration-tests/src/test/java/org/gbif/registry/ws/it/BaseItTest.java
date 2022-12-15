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

import org.gbif.api.vocabulary.UserRole;
import org.gbif.common.tests.database.DbConstants;
import org.gbif.common.tests.database.PostgresDBExtension;
import org.gbif.registry.database.RegistryDatabaseInitializer;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.fixtures.TestConstants;
import org.gbif.ws.client.ClientBuilder;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;
import org.gbif.ws.security.KeyStore;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import static org.gbif.registry.ws.it.fixtures.TestConstants.IT_APP_KEY2;

/** Base class for IT tests that initializes data sources and basic security settings. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = RegistryIntegrationTestsConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext
public class BaseItTest {

//  @RegisterExtension
//  protected static PostgresDBExtension database =
//      PostgresDBExtension.builder()
//          .liquibaseChangeLogFile(TestConstants.LIQUIBASE_MASTER_FILE)
//          .initializer(new RegistryDatabaseInitializer())
//          .reuseLabel(DbConstants.REGISTRY_PG_CONTAINER_LABEL)
//          .build();

  public static PostgreSQLContainer CONTAINER;
  static {
    CONTAINER = new PostgreSQLContainer("postgres:11.1").withDatabaseName("registry");
    CONTAINER.withReuse(true).withLabel("reuse.tag", "registry_its_pg_container");
    CONTAINER.setWaitStrategy(
      Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofSeconds(60)));
    CONTAINER.start();

    Database databaseLiquibase = null;
    try {
      databaseLiquibase = DatabaseFactory.getInstance()
        .findCorrectDatabaseImplementation(new JdbcConnection(CONTAINER.createConnection("")));
      Liquibase liquibase =
        new Liquibase(TestConstants.LIQUIBASE_MASTER_FILE, new ClassLoaderResourceAccessor(), databaseLiquibase);
      liquibase.update(new Contexts());

      new RegistryDatabaseInitializer().init(CONTAINER.createConnection(""));
    } catch (DatabaseException e) {
      throw new RuntimeException(e);
    } catch (SQLException | LiquibaseException e) {
      throw new RuntimeException(e);
    }
  }

  private final SimplePrincipalProvider simplePrincipalProvider;

  protected static EsManageServer esServer;

  public BaseItTest(SimplePrincipalProvider simplePrincipalProvider, EsManageServer esServer) {
    this.simplePrincipalProvider = simplePrincipalProvider;
    BaseItTest.esServer = esServer;
  }

  @BeforeEach
  public void setup() {
    // reset SimplePrincipleProvider, configured for web resources tests only
    if (simplePrincipalProvider != null) {
      simplePrincipalProvider.setPrincipal(TestConstants.TEST_ADMIN);
      SecurityContext ctx = SecurityContextHolder.createEmptyContext();
      SecurityContextHolder.setContext(ctx);
      ctx.setAuthentication(
          new UsernamePasswordAuthenticationToken(
              simplePrincipalProvider.get().getName(),
              "",
              Arrays.asList(
                  new SimpleGrantedAuthority(UserRole.REGISTRY_ADMIN.name()),
                  new SimpleGrantedAuthority(UserRole.GRSCICOLL_ADMIN.name()))));
    }
  }

  protected void resetSecurityContext(String principal, UserRole role) {
    simplePrincipalProvider.setPrincipal(principal);
    SecurityContext ctx = SecurityContextHolder.createEmptyContext();
    SecurityContextHolder.setContext(ctx);
    ctx.setAuthentication(
        new UsernamePasswordAuthenticationToken(
            simplePrincipalProvider.get().getName(),
            "",
            Collections.singleton(new SimpleGrantedAuthority(role.name()))));
  }

  public SimplePrincipalProvider getSimplePrincipalProvider() {
    return simplePrincipalProvider;
  }

  public enum ServiceType {
    RESOURCE,
    CLIENT
  }

  protected <T> T prepareClient(int localServerPort, KeyStore keyStore, Class<T> cls) {
    return prepareClient(IT_APP_KEY2, IT_APP_KEY2, localServerPort, keyStore, cls);
  }

  protected <T> T prepareClient(
      String username, int localServerPort, KeyStore keyStore, Class<T> cls) {
    return prepareClient(username, IT_APP_KEY2, localServerPort, keyStore, cls);
  }

  protected <T> T prepareClient(
      String username, String appKey, int localServerPort, KeyStore keyStore, Class<T> cls) {
    ClientBuilder clientBuilder = new ClientBuilder();
    clientBuilder.withObjectMapper(
        JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport());
    return clientBuilder
        .withUrl("http://localhost:" + localServerPort)
        .withAppKeyCredentials(username, appKey, keyStore.getPrivateKey(appKey))
        .build(cls);
  }

  protected <T> T getService(ServiceType param, T resource, T client) {
    switch (param) {
      case CLIENT:
        return client;
      case RESOURCE:
        return resource;
      default:
        throw new IllegalStateException("Must be resource or client");
    }
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("registry.datasource.url", () -> CONTAINER.getJdbcUrl());
    registry.add(
        "registry.datasource.username", () -> CONTAINER.getUsername());
    registry.add(
        "registry.datasource.password", () -> CONTAINER.getPassword());
    registry.add("elasticsearch.mock", () -> "true");
  }
}
