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

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.Builder;
import lombok.Singular;
import lombok.SneakyThrows;

@Builder
public class PostgresDBExtension implements BeforeAllCallback, AfterAllCallback {

  private static final String DB = "registry";
  private static final String POSTGRES_IMAGE = "postgres:11.1";

  protected static final PostgreSQLContainer CONTAINER;

  private String liquibaseChangeLogFile;
  @Singular private List<DBInitializer> initializers;

  static {
    CONTAINER = new PostgreSQLContainer(POSTGRES_IMAGE).withDatabaseName(DB);
    CONTAINER.withReuse(true).withLabel("reuse.UUID", "e06d7a87-7d7d-472e-a047-e6c81f61d2a4");
    CONTAINER.setWaitStrategy(
        Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofSeconds(60)));
  }

  @SneakyThrows
  @Override
  public void beforeAll(ExtensionContext context) {
    CONTAINER.start();

    if (liquibaseChangeLogFile != null) {
      updateLiquibase();
    }

    if (initializers != null) {
      for (DBInitializer initializer : initializers) {
        initializer.init(CONTAINER.createConnection(""));
      }
    }
  }

  @Override
  public void afterAll(ExtensionContext context) {
    CONTAINER.stop();
  }

  public PostgreSQLContainer getPostgresContainer() {
    return CONTAINER;
  }

  @SneakyThrows
  private void updateLiquibase() {
    Database databaseLiquibase =
        DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(CONTAINER.createConnection("")));
    Liquibase liquibase =
        new Liquibase(liquibaseChangeLogFile, new ClassLoaderResourceAccessor(), databaseLiquibase);
    liquibase.update(new Contexts());
  }
}
