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
package org.gbif.registry.ws.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.zaxxer.hikari.HikariDataSource;

/** Contains all datasources required. */
@Configuration
public class DataSourcesConfiguration {

  public static final String REGISTRY_DATASOURCE_PREFIX = "registry";

  @Bean
  @Primary
  @ConfigurationProperties(REGISTRY_DATASOURCE_PREFIX + ".datasource")
  public DataSourceProperties registryDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  @Primary
  @ConfigurationProperties("registry.datasource.hikari")
  public HikariDataSource registryDataSource() {
    return registryDataSourceProperties()
        .initializeDataSourceBuilder()
        .type(HikariDataSource.class)
        .build();
  }

  @Bean
  @ConfigurationProperties("indexing.datasource.checklistbank")
  public DataSourceProperties clbDataSourceProperties() {
    return new DataSourceProperties();
  }

  /** Datasource required for dataset indexing. */
  @Bean(name = "clb_datasource")
  @ConfigurationProperties("indexing.datasource.checklistbank.hikari")
  public HikariDataSource clbDataSource() {
    return clbDataSourceProperties()
        .initializeDataSourceBuilder()
        .type(HikariDataSource.class)
        .build();
  }
}
