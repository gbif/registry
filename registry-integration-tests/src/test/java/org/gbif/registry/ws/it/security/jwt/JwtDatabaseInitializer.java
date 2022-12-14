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
package org.gbif.registry.ws.it.security.jwt;

import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.identity.util.RegistryPasswordEncoder;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;

import lombok.SneakyThrows;

import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_PASSWORD;

/** DB initialization needed for JWT tests. */
public class JwtDatabaseInitializer implements BeforeAllCallback {

  private static final RegistryPasswordEncoder ENCODER = new RegistryPasswordEncoder();
  static final String ADMIN_USER = "administrator";
  static final String TEST_USER = "testuser";
  static final String GRSCICOLL_ADMIN = "grscicolladmin";

  private final PostgreSQLContainer postgreSQLContainer;

  public JwtDatabaseInitializer(PostgreSQLContainer postgreSQLContainer) {
    this.postgreSQLContainer = postgreSQLContainer;
  }

  @SneakyThrows
  @Override
  public void beforeAll(ExtensionContext extensionContext) {
    // add users
    addUsers(postgreSQLContainer.createConnection(""));
  }

  @SneakyThrows
  private void addUsers(Connection connection) {
    connection
        .prepareStatement(
            "DELETE FROM public.\"user\" WHERE username IN ('"
                + ADMIN_USER
                + "','"
                + TEST_USER
                + "','"
                + GRSCICOLL_ADMIN
                + "')")
        .executeUpdate();
    connection
        .prepareStatement(
            createInsertUserQuery(
                ADMIN_USER,
                TEST_PASSWORD,
                Arrays.asList(UserRole.USER, UserRole.REGISTRY_ADMIN, UserRole.REGISTRY_EDITOR)))
        .executeUpdate();
    connection
        .prepareStatement(
            createInsertUserQuery(
                TEST_USER, TEST_PASSWORD, Collections.singletonList(UserRole.USER)))
        .executeUpdate();
    connection
        .prepareStatement(
            createInsertUserQuery(
                GRSCICOLL_ADMIN,
                TEST_PASSWORD,
                Collections.singletonList(UserRole.GRSCICOLL_ADMIN)))
        .executeUpdate();
    connection.close();
  }

  private String createInsertUserQuery(String user, String password, List<UserRole> roles) {
    return "INSERT INTO public.\"user\" (username, email, password, first_name, last_name, roles, settings, system_settings, created, last_login, deleted, challenge_code_key) "
        + "VALUES ('"
        + user
        + "', '"
        + user
        + "@test.com', '"
        + ENCODER.encode(password)
        + "', '"
        + user
        + "', null, '{"
        + roles.stream().map(UserRole::name).collect(Collectors.joining(","))
        + "}', 'country => DK', '', '2022-05-08 13:30:04.833025', '2022-08-04 23:20:30.330778', null, null)";
  }
}
