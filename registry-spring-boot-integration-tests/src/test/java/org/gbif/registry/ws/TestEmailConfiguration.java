package org.gbif.registry.ws;

import org.gbif.registry.mail.EmailSender;
import org.gbif.registry.surety.email.InMemoryEmailSender;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestEmailConfiguration {

  @Bean
  @Primary
  public EmailSender emailSender() {
    return new InMemoryEmailSender();
  }
}
