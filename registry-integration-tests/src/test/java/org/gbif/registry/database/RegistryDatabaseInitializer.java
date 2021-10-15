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

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

import lombok.experimental.UtilityClass;

/**
 * A Rule that initializes a database for All test cases in a Test class or in test suite.
 *
 * <pre>
 *  RegistryDatabaseInitializer().init();
 * </pre>
 */
@UtilityClass
public class RegistryDatabaseInitializer {

  private static final Logger LOG = LoggerFactory.getLogger(RegistryDatabaseInitializer.class);

  public static void init(DataSource dataSource) {
    LOG.info("Create test users");
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);

      connection
          .prepareStatement(
              "INSERT INTO public.\"user\" (key, username, email, password, first_name, last_name, roles, settings, system_settings, created, last_login, deleted, challenge_code_key) "
                  + "VALUES "
                  + "(-1, 'gbif.app.it2', 'gbif.app.it2@mailinator.com', '$S$DSLeulP5GbaEzGpqDSJJVG8mFUisQP.Bmy/S15VVbG9aadZQ6KNp', null, null, '{GRSCICOLL_ADMIN,REGISTRY_ADMIN}', 'country => DK', '', '2019-05-08 13:30:04.833025', '2020-04-04 23:20:30.330778', null, null),"
                  + "(-2, 'editor', 'editor2@mailinator.com', '$S$DIU6YGMU7aKb0rISEEqtePk.PwJPU.z.f5G0Au426gIJVd5RS8xs', null, null, '{USER,REGISTRY_EDITOR}', 'country => DK', '', '2019-05-08 13:30:04.833025', '2020-04-04 23:20:30.330778', null, null)")
          .executeUpdate();

      connection.commit();

    } catch (SQLException e) {
      Throwables.propagate(e);
    }
    LOG.info("Initial test data loaded");
  }
}
