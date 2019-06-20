package org.gbif.registry.cli;

import org.gbif.registry.cli.common.DataCiteConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("org.gbif.registry.persistence.mapper")
@EnableConfigurationProperties(DataCiteConfiguration.class)
public class RegistryCliApplication {
  public static void main(String[] args) {
    SpringApplication.run(RegistryCliApplication.class, args);
  }
}
