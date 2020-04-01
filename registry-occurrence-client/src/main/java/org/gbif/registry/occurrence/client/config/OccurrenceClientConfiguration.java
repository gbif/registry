/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
