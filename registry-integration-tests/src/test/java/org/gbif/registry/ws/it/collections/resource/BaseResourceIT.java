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
package org.gbif.registry.ws.it.collections.resource;

import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.mocks.IdentityServiceMock;
import org.gbif.registry.ws.it.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.ws.it.fixtures.RequestTestFixture;
import org.gbif.registry.ws.it.fixtures.TestConstants;
import org.gbif.ws.client.ClientBuilder;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;
import org.gbif.ws.security.GbifAuthService;
import org.gbif.ws.security.GbifAuthenticationManager;
import org.gbif.ws.security.GbifAuthenticationManagerImpl;
import org.gbif.ws.security.KeyStore;

import java.util.Arrays;
import java.util.Collections;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;

import static org.gbif.registry.ws.it.fixtures.TestConstants.IT_APP_KEY2;

/** Base class for IT tests that initializes data sources and basic security settings. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = {RegistryIntegrationTestsConfiguration.class, BaseResourceIT.MockConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {BaseResourceIT.EsContainerContextInitializer.class})
@ActiveProfiles({"test", "mock"})
@AutoConfigureMockMvc
public class BaseResourceIT {

  public static class EsContainerContextInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of("elasticsearch.mock=true")
          .applyTo(configurableApplicationContext.getEnvironment());
    }
  }

  protected static EsManageServer esServer;
  private final SimplePrincipalProvider simplePrincipalProvider;
  protected final RequestTestFixture requestTestFixture;

  public BaseResourceIT(
      SimplePrincipalProvider simplePrincipalProvider, RequestTestFixture requestTestFixture) {
    this.simplePrincipalProvider = simplePrincipalProvider;
    this.requestTestFixture = requestTestFixture;
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

  protected <T> T prepareClient(String username, int localServerPort, Class<T> cls) {
    ClientBuilder clientBuilder = new ClientBuilder();
    clientBuilder.withObjectMapper(
        JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport());
    return clientBuilder
        .withUrl("http://localhost:" + localServerPort)
        .withCredentials(username, username)
        .build(cls);
  }

  @TestConfiguration
  @Profile("mock")
  public static class MockConfig {

    @Qualifier("registryDataSource")
    @MockBean
    DataSource dataSource;

    @MockBean PlatformTransactionManager platformTransactionManager;

    @Primary
    @Bean
    GbifAuthenticationManager gbifAuthenticationManager() {
      return new GbifAuthenticationManagerImpl(
          new IdentityServiceMock(), Mockito.mock(GbifAuthService.class));
    }
  }
}
