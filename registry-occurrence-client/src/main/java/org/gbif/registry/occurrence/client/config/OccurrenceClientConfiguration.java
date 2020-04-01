package org.gbif.registry.occurrence.client.config;

import org.gbif.registry.occurrence.client.OccurrenceMetricsClient;
import org.gbif.ws.client.ClientFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OccurrenceClientConfiguration {

  private ClientFactory clientFactory;

  public OccurrenceClientConfiguration(@Value("${occurrence.ws.url}") String url) {
    this.clientFactory = new ClientFactory(url);
  }

  @Bean
  public OccurrenceMetricsClient occurrenceMetricsClient() {
    return clientFactory.newInstance(OccurrenceMetricsClient.class);
  }
}
