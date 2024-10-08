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
package org.gbif.registry.ws.it.collections.service;

import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.database.BaseDBTest;
import org.gbif.registry.events.collections.AuditLogger;
import org.gbif.registry.ws.it.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.ws.it.fixtures.RequestTestFixture;
import org.gbif.registry.ws.it.fixtures.TestConstants;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Base class for IT tests that initializes data sources and basic security settings. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = {RegistryIntegrationTestsConfiguration.class, BaseServiceIT.MockConfig.class})
@ActiveProfiles({"test", "mock"})
public class BaseServiceIT extends BaseDBTest {

  // the audit log is tested in the AuditLogIT
  @MockBean private AuditLogger auditLogger;

  private final SimplePrincipalProvider simplePrincipalProvider;

  public BaseServiceIT(SimplePrincipalProvider simplePrincipalProvider) {
    this.simplePrincipalProvider = simplePrincipalProvider;
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

  /**
   * Resets the Spring Security context with a given principal and user roles.
   *
   * <p>This method sets the specified principal (user identifier) and roles
   * for the security context, effectively simulating an authenticated user
   * in the system for testing or execution purposes. It clears the existing
   * security context and creates a new one with the provided principal and roles.
   *
   * @param principal The principal (typically the username or identifier of the user) to set in the security context.
   * @param role      One or more {@link UserRole} values representing the roles or authorities to assign to the principal.
   */
  protected void resetSecurityContext(String principal, UserRole... role) {
    simplePrincipalProvider.setPrincipal(principal);
    SecurityContext ctx = SecurityContextHolder.createEmptyContext();
    SecurityContextHolder.setContext(ctx);
    List<SimpleGrantedAuthority> authorities =
        Arrays.stream(role)
            .map(r -> new SimpleGrantedAuthority(r.name()))
            .collect(Collectors.toList());

    ctx.setAuthentication(
        new UsernamePasswordAuthenticationToken(
            simplePrincipalProvider.get().getName(), "", authorities));
  }

  public SimplePrincipalProvider getSimplePrincipalProvider() {
    return simplePrincipalProvider;
  }

  @TestConfiguration
  @Profile("mock")
  public static class MockConfig {

    @MockBean RequestTestFixture requestTestFixture;
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("registry.datasource.url", PG_CONTAINER::getJdbcUrl);
    registry.add("registry.datasource.username", PG_CONTAINER::getUsername);
    registry.add("registry.datasource.password", PG_CONTAINER::getPassword);
    registry.add("elasticsearch.mock", () -> "true");
  }
}
