package org.gbif.registry.doi.config;

import org.gbif.occurrence.query.TitleLookupService;
import org.gbif.occurrence.query.TitleLookupServiceFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TitleLookupConfiguration {

  @Value("${api.root.url}")
  private String apiRoot;

  @Bean
  public TitleLookupService titleLookupService() {
    return TitleLookupServiceFactory.getInstance(apiRoot);
  }
}
