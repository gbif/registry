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
package org.gbif.registry.db.it;

import org.gbif.registry.ws.it.RegistryIntegrationTestsConfiguration;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.zaxxer.hikari.HikariDataSource;

import liquibase.integration.spring.SpringLiquibase;

/**
 * Runs the liquibase change logs against an external database.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {DbMigrationTest.Configuration.class})
@PropertySource(RegistryIntegrationTestsConfiguration.TEST_PROPERTIES)
@ActiveProfiles("test")
@EnableAutoConfiguration(
    exclude = {DataSourceAutoConfiguration.class, RabbitAutoConfiguration.class})
public class DbMigrationTest {

  @Autowired SpringLiquibase springLiquibase;

  @Test
  public void testDbMigration() {
    Assertions.assertNotNull(springLiquibase);
  }

  @TestConfiguration
  @SpringBootConfiguration
  public static class Configuration {

    @Bean
    @Primary
    @ConfigurationProperties("dbmigration.datasource")
    public DataSourceProperties dataSourceProperties() {
      DataSourceProperties dataSourceProperties = new DataSourceProperties();
      dataSourceProperties.setDriverClassName("org.postgresql.Driver");
      return dataSourceProperties;
    }

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "dbmigration.datasource")
    public HikariDataSource dataSource() {
      return dataSourceProperties()
          .initializeDataSourceBuilder()
          .driverClassName("org.postgresql.Driver")
          .type(HikariDataSource.class)
          .build();
    }

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "dbmigration.liquibase")
    public LiquibaseProperties liquibaseProperties() {
      return new LiquibaseProperties();
    }

    @Bean
    @Primary
    public SpringLiquibase liquibase() {
      return springLiquibase(dataSource(), liquibaseProperties());
    }

    private static SpringLiquibase springLiquibase(
        DataSource dataSource, LiquibaseProperties properties) {
      SpringLiquibase liquibase = new SpringLiquibase();
      liquibase.setDataSource(dataSource);
      liquibase.setChangeLog(properties.getChangeLog());
      liquibase.setContexts(properties.getContexts());
      liquibase.setDefaultSchema(properties.getDefaultSchema());
      liquibase.setDropFirst(properties.isDropFirst());
      liquibase.setShouldRun(properties.isEnabled());
      liquibase.setLabels(properties.getLabels());
      liquibase.setChangeLogParameters(properties.getParameters());
      liquibase.setRollbackFile(properties.getRollbackFile());
      return liquibase;
    }
  }
}
