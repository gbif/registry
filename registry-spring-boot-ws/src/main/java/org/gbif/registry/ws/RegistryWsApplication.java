package org.gbif.registry.ws;

import org.gbif.cli.indexing.dataset.DatasetBatchIndexBuilder;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.elasticsearch.ElasticSearchRestHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

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
    "org.gbif.registry.search.dataset.service",
    "org.gbif.registry.search.dataset.indexing",
    "org.gbif.registry.ws",
    "org.gbif.registry.persistence",
    "org.gbif.registry.identity",
    "org.gbif.registry.surety",
    "org.gbif.registry.mail",
    "org.gbif.registry.stubs",
    "org.gbif.registry.doi",
    "org.gbif.registry.pipelines",
    "org.gbif.registry.directory",
    "org.gbif.registry.events",
  },
  excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = DatasetBatchIndexBuilder.class)
  }
)
public class RegistryWsApplication {
  public static void main(String[] args) {
    SpringApplication.run(RegistryWsApplication.class, args);
  }
}
