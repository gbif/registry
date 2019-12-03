package org.gbif.registry;

import org.gbif.registry.mail.EmailSender;
import org.gbif.registry.mail.InMemoryEmailSender;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

@TestConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = {"org.gbif.ws.server.interceptor",
  "org.gbif.ws.server.filter", "org.gbif.ws.security", "org.gbif.query", "org.gbif.registry"})
@PropertySource("classpath:application-test.yml")
public class RegistryIntegrationTestsConfiguration {

  // use InMemoryEmailSender if devemail is disabled
  @Bean
  @ConditionalOnProperty(value = "mail.devemail.enabled", havingValue = "false")
  public EmailSender emailSender() {
    return new InMemoryEmailSender();
  }
}
