package org.gbif.registry.ws.config;

import com.google.common.eventbus.EventBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventConfiguration {

  @Bean
  public EventBus eventBus() {
    return new EventBus();
  }
}
