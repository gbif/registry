package org.gbif.registry;

import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.registry.mail.EmailSender;
import org.gbif.registry.mail.InMemoryEmailSender;
import org.gbif.registry.message.MessagePublisherStub;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

@TestConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = {"org.gbif.ws.server.interceptor",
  "org.gbif.ws.server.filter", "org.gbif.ws.security", "org.gbif.query", "org.gbif.registry"})
@PropertySource("classpath:application-test.yml")
public class RegistryIntegrationTestsConfiguration {

  // use InMemoryEmailSender if devemail is disabled
  @Bean
  @Primary
  @ConditionalOnProperty(value = "mail.devemail.enabled", havingValue = "false")
  public EmailSender emailSender() {
    return new InMemoryEmailSender();
  }

  // use stub instead of rabbit MQ if message is disabled
  @Bean
  @Primary
  @ConditionalOnProperty(value = "message.enabled", havingValue = "false")
  public MessagePublisher messagePublisher() {
    return new MessagePublisherStub();
  }
}
