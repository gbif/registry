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
package org.gbif.registry.cli.util;

import org.gbif.registry.cli.common.DbConfiguration;

import io.zonky.test.db.postgres.junit5.PreparedDbExtension;

/**
 * Utility class to manage configurations and constants related to the embedded postgres database.
 */
public class EmbeddedPostgresTestUtils {

  private EmbeddedPostgresTestUtils() {}

  public static final String LIQUIBASE_MASTER_FILE = "liquibase/master.xml";

  /** Extracts the connection information to create a {@link DbConfiguration} instance. */
  public static DbConfiguration toDbConfig(PreparedDbExtension database) {
    DbConfiguration db = new DbConfiguration();
    db.serverName = "localhost:" + database.getConnectionInfo().getPort();
    db.databaseName = database.getConnectionInfo().getDbName();
    db.user = database.getConnectionInfo().getUser();
    db.password = "";
    return db;
  }
}
