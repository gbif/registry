package org.gbif.registry.ws;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestSerializationConfiguration {

  @Bean
  public XmlMapper xmlMapper() {
    return new XmlMapper();
  }
}
