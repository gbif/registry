package org.gbif.registry.ws.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Contains all datasources required.
 */
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
    return registryDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
  }

  @Bean
  @ConfigurationProperties("indexing.datasource.checklistbank")
  public DataSourceProperties clbDataSourceProperties() {
    return new DataSourceProperties();
  }

  /**
   * Datasource required for dataset indexing.
   */
  @Bean(name = "clb_datasource")
  @ConfigurationProperties("indexing.datasource.checklistbank.hikari")
  public HikariDataSource clbDataSource() {
    return clbDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
  }
}
