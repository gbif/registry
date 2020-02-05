/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry;

import org.gbif.registry.ws.config.DataSourcesConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.rules.ExternalResource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * A JUnit rule to perform the necessary DB operations before or after running integration tests.ø
 */
public class DatabaseInitializer extends ExternalResource {

  private static final String INIT_DB_SCRIPT = "/init-db.sql";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());
  private static final ResourceLoader RESOURCE_LOADER = new DefaultResourceLoader();
  private static DataSource dataSource =
      createDataSourceFromProperties(RegistryIntegrationTestsConfiguration.TEST_PROPERTIES);

  @Override
  protected void before() throws Exception {
    initDB();
  }

  /** Runs a script to initiliaze the DB. */
  private void initDB() throws SQLException {
    // run the script
    Connection connection = dataSource.getConnection();
    ScriptUtils.executeSqlScript(connection, new ClassPathResource(INIT_DB_SCRIPT));
    connection.close();
  }

  private static DataSource createDataSourceFromProperties(String propertiesPath) {
    // read properties file
    JsonNode config = null;
    try {
      File configFile = RESOURCE_LOADER.getResource(propertiesPath).getFile();
      config = OBJECT_MAPPER.readTree(configFile);
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "Couldn't read properties from path: " + propertiesPath, e);
    }

    // get the datasource properties
    JsonNode datasourceConfig =
        config.get(DataSourcesConfiguration.REGISTRY_DATASOURCE_PREFIX).get("datasource");

    // create datasource
    DataSource dataSource =
        DataSourceBuilder.create()
            .url(datasourceConfig.get("url").asText())
            .username(datasourceConfig.get("username").asText())
            .password(datasourceConfig.get("password").asText())
            .build();

    return dataSource;
  }
}
