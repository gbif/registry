package org.gbif.registry;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.gbif.registry.persistence.mapper")
public class GbifRegistryApplication {
  public static void main(String[] args) {
    SpringApplication.run(GbifRegistryApplication.class, args);
  }
}
