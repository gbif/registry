/*
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
package org.gbif.registry.search.dataset.indexing.ws;


import org.gbif.checklistbank.ws.client.DatasetMetricsClient;
import org.gbif.checklistbank.ws.client.SpeciesResourceClient;
import org.gbif.metrics.ws.client.CubeWsClient;
import org.gbif.occurrence.ws.client.OccurrenceWsSearchClient;
import org.gbif.ws.client.ClientBuilder;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;


@Configuration
public class GbifApiServiceConfig {

  @Bean
  public OccurrenceWsSearchClient occurrenceWsSearchClient(
      @Value("${api.root.url}") String apiBaseUrl,
      @Qualifier("apiMapper") ObjectMapper objectMapper) {
    return new ClientBuilder()
            .withObjectMapper(objectMapper)
            .withUrl(apiBaseUrl)
            .build(OccurrenceWsSearchClient.class);
  }

  @Bean
  public CubeWsClient cubeWsClient(
    @Value("${api.root.url}") String apiBaseUrl,
    @Qualifier("apiMapper") ObjectMapper objectMapper) {
    return new ClientBuilder()
      .withObjectMapper(objectMapper)
      .withUrl(apiBaseUrl)
      .build(CubeWsClient.class);
  }

  @Bean
  public DatasetMetricsClient datasetMetricsClient(
    @Value("${api.root.url}") String apiBaseUrl,
    @Qualifier("apiMapper") ObjectMapper objectMapper) {
    return new ClientBuilder()
      .withObjectMapper(objectMapper)
      .withUrl(apiBaseUrl)
      .build(DatasetMetricsClient.class);
  }

  @Bean
  public SpeciesResourceClient speciesResourceClient(
    @Value("${api.root.url}") String apiBaseUrl,
    @Qualifier("apiMapper") ObjectMapper objectMapper) {
    return new ClientBuilder()
      .withObjectMapper(objectMapper)
      .withUrl(apiBaseUrl)
      .build(SpeciesResourceClient.class);
  }
}
