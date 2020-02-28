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
package org.gbif.registry.search.dataset.indexing.es;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class EsConfiguration {

  @Bean
  @Primary
  public RestHighLevelClient restHighLevelClient(
      @Value("${elasticsearch.registry.hosts}") String hosts,
      @Value("${elasticsearch.registry.connectionTimeOut}") int connectionTimeOut,
      @Value("${elasticsearch.registry.socketTimeOut}") int socketTimeOut,
      @Value("${elasticsearch.registry.connectionRequestTimeOut}") int connectionRequestTimeOut,
      @Value("${elasticsearch.registry.maxRetryTimeOut}") int maxRetryTimeOut) {
    return EsClient.provideEsClient(
        hosts.split(","),
        connectionTimeOut,
        socketTimeOut,
        connectionRequestTimeOut,
        maxRetryTimeOut);
  }

  @Bean(name = "occurrenceEsClient")
  public RestHighLevelClient occurrenceRestHighLevelClient(
      @Value("${elasticsearch.occurrence.hosts}") String hosts,
      @Value("${elasticsearch.occurrence.connectionTimeOut}") int connectionTimeOut,
      @Value("${elasticsearch.occurrence.socketTimeOut}") int socketTimeOut,
      @Value("${elasticsearch.occurrence.connectionRequestTimeOut}") int connectionRequestTimeOut,
      @Value("${elasticsearch.occurrence.maxRetryTimeOut}") int maxRetryTimeOut) {
    return EsClient.provideEsClient(
        hosts.split(","),
        connectionTimeOut,
        socketTimeOut,
        connectionRequestTimeOut,
        maxRetryTimeOut);
  }
}
