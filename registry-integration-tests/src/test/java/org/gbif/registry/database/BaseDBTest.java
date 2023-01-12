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

import org.gbif.registry.ws.it.fixtures.TestConstants;

import java.sql.SQLException;
import java.time.Duration;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

public class BaseDBTest {

  public static final PostgreSQLContainer PG_CONTAINER;

  static {
    PG_CONTAINER = new PostgreSQLContainer("postgres:11.1").withDatabaseName("registry");
    PG_CONTAINER.withReuse(true).withLabel("reuse.tag", "registry_ITs_PG_container");
    PG_CONTAINER.setWaitStrategy(
        Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofSeconds(60)));
    PG_CONTAINER.start();

    try {
      updateLiquibase();
      RegistryDatabaseInitializer.init(PG_CONTAINER.createConnection(""));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void updateLiquibase() throws SQLException, LiquibaseException {
    Database databaseLiquibase;
    databaseLiquibase =
        DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(
                new JdbcConnection(PG_CONTAINER.createConnection("")));
    Liquibase liquibase =
        new Liquibase(
            TestConstants.LIQUIBASE_MASTER_FILE,
            new ClassLoaderResourceAccessor(),
            databaseLiquibase);
    liquibase.update(new Contexts());
  }
}
