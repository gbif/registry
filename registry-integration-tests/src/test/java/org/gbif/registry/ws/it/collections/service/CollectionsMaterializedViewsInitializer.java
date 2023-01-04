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

import java.sql.Connection;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.PostgreSQLContainer;

import lombok.SneakyThrows;

public class CollectionsMaterializedViewsInitializer implements BeforeAllCallback {

  private PostgreSQLContainer postgreSQLContainer;

  public CollectionsMaterializedViewsInitializer() {}

  public CollectionsMaterializedViewsInitializer(PostgreSQLContainer postgreSQLContainer) {
    this.postgreSQLContainer = postgreSQLContainer;
  }

  @SneakyThrows
  public void init(Connection connection) {
    // create materialized view for testing
    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/create_duplicates_views.sql"));
    connection.close();
  }

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
    init(postgreSQLContainer.createConnection(""));
  }
}
