package org.gbif.registry;

import org.gbif.cli.indexing.dataset.DatasetBatchIndexBuilder;

import com.zaxxer.hikari.HikariDataSource;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.elasticsearch.ElasticSearchRestHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;

@SpringBootApplication(
  exclude = {
    ElasticsearchAutoConfiguration.class,
    ElasticSearchRestHealthContributorAutoConfiguration.class
  })
@MapperScan("org.gbif.registry.persistence.mapper")
@EnableConfigurationProperties
@ComponentScan(
  basePackages = {
    "org.gbif.ws.server.interceptor",
    "org.gbif.ws.server.aspect",
    "org.gbif.ws.server.filter",
    "org.gbif.ws.server.advice",
    "org.gbif.ws.server.mapper",
    "org.gbif.ws.security",
    "org.gbif.registry"
  },
  excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = DatasetBatchIndexBuilder.class)
  }
)
public class RegistryWsApplication {
  public static void main(String[] args) {
    SpringApplication.run(RegistryWsApplication.class, args);
  }

  @Bean
  @Primary
  @ConfigurationProperties("registry.datasource")
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
