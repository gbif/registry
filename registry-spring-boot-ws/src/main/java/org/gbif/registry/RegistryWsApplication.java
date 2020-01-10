package org.gbif.registry;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.solr.SolrHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(
  scanBasePackages = {
    "org.gbif.ws.server.interceptor",
    "org.gbif.ws.server.aspect",
    "org.gbif.ws.server.filter",
    "org.gbif.ws.security",
    "org.gbif.query",
    "org.gbif.registry"},
  exclude = {
    SolrAutoConfiguration.class,
    SolrHealthContributorAutoConfiguration.class
  })
@MapperScan("org.gbif.registry.persistence.mapper")
@EnableConfigurationProperties
public class RegistryWsApplication {
  public static void main(String[] args) {
    SpringApplication.run(RegistryWsApplication.class, args);
  }
}
