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
import org.gbif.registry.database.RegistryDatabaseInitializer;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.fixtures.TestConstants;
import org.gbif.ws.client.ClientBuilder;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
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

import io.zonky.test.db.postgres.embedded.ConnectionInfo;
import io.zonky.test.db.postgres.embedded.DatabasePreparer;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import io.zonky.test.db.postgres.embedded.LiquibasePreparer;
import io.zonky.test.db.postgres.embedded.PreparedDbProvider;

import static org.gbif.registry.ws.it.fixtures.TestConstants.IT_APP_KEY2;

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

  public static class EsContainerContextInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of("elasticsearch.mock=false")
          .applyTo(configurableApplicationContext.getEnvironment());
    }
  }

  /** Prepares a Tests database using an embedded Postgres instance. */
  public static class EmbeddedDataBaseInitializer {
    private final DataSource dataSource;
    private final PreparedDbProvider provider;
    private final ConnectionInfo connectionInfo;
    private final List<Consumer<EmbeddedPostgres.Builder>> builderCustomizers =
        new CopyOnWriteArrayList();

    public EmbeddedDataBaseInitializer(DatabasePreparer preparer) {
      try {
        this.provider = PreparedDbProvider.forPreparer(preparer, this.builderCustomizers);
        this.connectionInfo = provider.createNewDatabase();
        this.dataSource = provider.createDataSourceFromConnectionInfo(this.connectionInfo);
      } catch (SQLException ex) {
        throw new RuntimeException(ex);
      }
    }

    public DataSource getDataSource() {
      return dataSource;
    }

    public ConnectionInfo getConnectionInfo() {
      return connectionInfo;
    }
  }

  /** Custom ContextInitializer to expose the registry DB data source and search flags. */
  public static class ContextInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {

      EmbeddedDataBaseInitializer database =
          new EmbeddedDataBaseInitializer(
              LiquibasePreparer.forClasspathLocation(TestConstants.LIQUIBASE_MASTER_FILE));

      RegistryDatabaseInitializer.init(database.getDataSource());

      TestPropertyValues.of(
              Stream.of(dbTestPropertyPairs(database)).flatMap(Stream::of).toArray(String[]::new))
          .applyTo(configurableApplicationContext.getEnvironment());

      TestPropertyValues.of("elasticsearch.mock=true")
          .applyTo(configurableApplicationContext.getEnvironment());
    }

    /** Creates the registry datasource settings from the embedded database. */
    String[] dbTestPropertyPairs(EmbeddedDataBaseInitializer database) {
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
}
