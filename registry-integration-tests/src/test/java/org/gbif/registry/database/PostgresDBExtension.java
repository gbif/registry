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

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class PostgresDBExtension implements BeforeAllCallback, AfterAllCallback {

  private static final String DB = "registry";
  private static final String POSTGRES_IMAGE = "postgres:11.1";

  protected static final PostgreSQLContainer CONTAINER;

  static {
    CONTAINER =
        (PostgreSQLContainer)
            new PostgreSQLContainer(POSTGRES_IMAGE)
                .withDatabaseName(DB)
                .withEnv("POSTGRESQL_DATABASE", DB);
    CONTAINER.withReuse(true).withLabel("reuse.UUID", "e06d7a87-7d7d-472e-a047-e6c81f61d2a4");
    CONTAINER.setWaitStrategy(
        Wait.defaultWaitStrategy().withStartupTimeout(Duration.ofSeconds(60)));

    CONTAINER.start();
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    CONTAINER.start();
  }

  @Override
  public void afterAll(ExtensionContext context) {
    CONTAINER.stop();
  }

  public PostgreSQLContainer getPostgresContainer() {
    return CONTAINER;
  }

  public DataSource getDatasoruce() {
    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(CONTAINER.getJdbcUrl());
    hikariConfig.setUsername(CONTAINER.getUsername());
    hikariConfig.setPassword(CONTAINER.getPassword());
    hikariConfig.setDriverClassName(CONTAINER.getDriverClassName());
    return new HikariDataSource(hikariConfig);
  }
}
