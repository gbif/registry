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
package org.gbif.registry.cli.common.spring;

import org.gbif.registry.cli.common.DbConfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceInitializationMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.zaxxer.hikari.HikariDataSource;

/** Contains all datasources required. */
@Configuration
@ConditionalOnBean(DbConfiguration.class)
public class DataSourcesConfiguration {

  @Autowired private DbConfiguration dbConfiguration;

  @Bean
  @Primary
  public DataSourceProperties buildDataSourceProperties() {
    DataSourceProperties dataSourceProperties = new DataSourceProperties();
    dataSourceProperties.setGenerateUniqueName(true);
    dataSourceProperties.setUsername(dbConfiguration.user);
    dataSourceProperties.setPassword(dbConfiguration.password);
    dataSourceProperties.setUrl(
        "jdbc:postgresql://" + dbConfiguration.serverName + "/" + dbConfiguration.databaseName);
    dataSourceProperties.setInitializationMode(DataSourceInitializationMode.ALWAYS);
    return dataSourceProperties;
  }

  /** Builds a Hikari DataSource using a prefix to get the configuration settings, */
  @Bean
  @Primary
  public HikariDataSource buildDataSource() {
    DataSourceProperties dataSourceProperties = buildDataSourceProperties();

    HikariDataSource dataSource =
        dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    dataSource.setIdleTimeout(6_000);
    dataSource.setMaximumPoolSize(dbConfiguration.maximumPoolSize);
    dataSource.setMinimumIdle(1);

    return dataSource;
  }
}
