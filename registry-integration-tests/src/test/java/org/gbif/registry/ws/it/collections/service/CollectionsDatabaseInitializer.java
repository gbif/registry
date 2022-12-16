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

import org.gbif.registry.identity.util.RegistryPasswordEncoder;

import java.sql.Connection;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;

import lombok.SneakyThrows;

import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_GRSCICOLL_ADMIN;
import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_PASSWORD;

/** DB initialization needed for collections tests. */
public class CollectionsDatabaseInitializer implements BeforeAllCallback {

  private static final RegistryPasswordEncoder ENCODER = new RegistryPasswordEncoder();

  private PostgreSQLContainer postgreSQLContainer;

  public CollectionsDatabaseInitializer() {}

  public CollectionsDatabaseInitializer(PostgreSQLContainer postgreSQLContainer) {
    this.postgreSQLContainer = postgreSQLContainer;
  }

  @SneakyThrows
  public void init(Connection connection) {
    connection
        .prepareStatement(
            "DELETE FROM public.\"user\" WHERE username = '" + TEST_GRSCICOLL_ADMIN + "'")
        .executeUpdate();
    connection
        .prepareStatement(
            "INSERT INTO public.\"user\" (username, email, password, first_name, last_name, roles, settings, system_settings, created, last_login, deleted, challenge_code_key) "
                + "VALUES ('"
                + TEST_GRSCICOLL_ADMIN
                + "', '"
                + TEST_GRSCICOLL_ADMIN
                + "@test.com', '"
                + ENCODER.encode(TEST_PASSWORD)
                + "', '"
                + TEST_GRSCICOLL_ADMIN
                + "', null, '{GRSCICOLL_ADMIN}', 'country => DK', '', '2022-05-08 13:30:04.833025', '2022-08-04 23:20:30.330778', null, null)")
        .executeUpdate();
    connection.close();
  }

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
    init(postgreSQLContainer.createConnection(""));
  }
}
