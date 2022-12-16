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
package org.gbif.registry.database;

import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.identity.util.RegistryPasswordEncoder;
import org.gbif.registry.ws.it.fixtures.TestConstants;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import com.google.common.base.Throwables;

import static org.gbif.api.vocabulary.UserRole.GRSCICOLL_ADMIN;
import static org.gbif.api.vocabulary.UserRole.REGISTRY_EDITOR;
import static org.gbif.registry.ws.it.fixtures.TestConstants.IT_APP_KEY2;
import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_ADMIN;
import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_GRSCICOLL_ADMIN;
import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_PASSWORD;
import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_USER;

/**
 * A Rule that initializes a database for All test cases in a Test class or in test suite.
 *
 * <pre>
 *  RegistryDatabaseInitializer().init();
 * </pre>
 */
public class RegistryDatabaseInitializer implements BeforeAllCallback {

  private static final RegistryPasswordEncoder ENCODER = new RegistryPasswordEncoder();

  private static final Logger LOG = LoggerFactory.getLogger(RegistryDatabaseInitializer.class);

  private PostgreSQLContainer container;

  public RegistryDatabaseInitializer(PostgreSQLContainer container) {
    this.container = container;
  }

  public static void init(Connection connection) {
    LOG.info("Create test users");
    try {
      connection.setAutoCommit(false);

      connection.prepareStatement("TRUNCATE public.\"user\" CASCADE").executeUpdate();
      connection.commit();

      connection
          .prepareStatement(
              createInsertUserQuery(
                  -1,
                  IT_APP_KEY2,
                  "$S$DSLeulP5GbaEzGpqDSJJVG8mFUisQP.Bmy/S15VVbG9aadZQ6KNp",
                  Arrays.asList(UserRole.REGISTRY_ADMIN, GRSCICOLL_ADMIN)))
          .executeUpdate();
      connection
          .prepareStatement(
              createInsertUserQuery(
                  -2,
                  TestConstants.TEST_EDITOR,
                  "$S$DIU6YGMU7aKb0rISEEqtePk.PwJPU.z.f5G0Au426gIJVd5RS8xs",
                  Arrays.asList(UserRole.USER, REGISTRY_EDITOR)))
          .executeUpdate();
      connection
          .prepareStatement(
              createInsertUserQuery(
                  -3,
                  TEST_ADMIN,
                  ENCODER.encode(TEST_PASSWORD),
                  Arrays.asList(UserRole.USER, UserRole.REGISTRY_ADMIN, UserRole.REGISTRY_EDITOR)))
          .executeUpdate();
      connection
          .prepareStatement(
              createInsertUserQuery(
                  -4,
                  TEST_USER,
                  ENCODER.encode(TEST_PASSWORD),
                  Collections.singletonList(UserRole.USER)))
          .executeUpdate();
      connection
          .prepareStatement(
              createInsertUserQuery(
                  -5,
                  TEST_GRSCICOLL_ADMIN,
                  ENCODER.encode(TEST_PASSWORD),
                  Collections.singletonList(GRSCICOLL_ADMIN)))
          .executeUpdate();
      connection.commit();
    } catch (SQLException e) {
      Throwables.propagate(e);
    } finally {
      try {
        connection.close();
      } catch (SQLException e) {
        Throwables.propagate(e);
      }
    }
    LOG.info("Initial test data loaded");
  }

  private static String createInsertUserQuery(
      int key, String user, String password, List<UserRole> roles) {
    return "INSERT INTO public.\"user\" (key, username, email, password, first_name, last_name, roles, settings, system_settings, created, last_login, deleted, challenge_code_key) "
        + "VALUES ("
        + key
        + ", '"
        + user
        + "', '"
        + user
        + "@test.com', '"
        + password
        + "', '"
        + user
        + "', null, '{"
        + roles.stream().map(UserRole::name).collect(Collectors.joining(","))
        + "}', 'country => DK', '', '2022-05-08 13:30:04.833025', '2022-08-04 23:20:30.330778', null, null)";
  }

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
    init(container.createConnection(""));
  }
}
