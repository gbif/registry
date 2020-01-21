package org.gbif.registry;

import org.gbif.registry.search.dataset.indexing.DatasetBatchIndexBuilder;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@MapperScan("org.gbif.registry.persistence.mapper")
@EnableConfigurationProperties
@ComponentScan( basePackages = {
  "org.gbif.ws.server.interceptor",
  "org.gbif.ws.server.aspect",
  "org.gbif.ws.server.filter",
  "org.gbif.ws.server.advice",
  "org.gbif.ws.server.mapper",
  "org.gbif.ws.security",
  "org.gbif.registry"
},
  excludeFilters = {
    @ComponentScan.Filter(type= FilterType.ASSIGNABLE_TYPE, value=DatasetBatchIndexBuilder.class)}
)
public class RegistryWsApplication {
  public static void main(String[] args) {
    SpringApplication.run(RegistryWsApplication.class, args);
  }
}
