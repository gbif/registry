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
package org.gbif.registry.search.test;

import org.gbif.registry.search.dataset.indexing.es.EsClient;

import org.elasticsearch.client.RestHighLevelClient;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;

@Configuration
public class DatasetElasticsearchConfiguration {

  @Autowired private ResourceLoader resourceLoader;

  @Bean("datasetElasticCluster")
  public EsManageServer esManageServer(
      @Value("${elasticsearch.registry.index}") String indexName,
      @Value("${elasticsearch.mock}") boolean mockEs) {
    try {
      return mockEs
          ? Mockito.mock(EsManageServer.class)
          : new EsManageServer(
              resourceLoader.getResource("classpath:dataset-es-mapping.json"),
              resourceLoader.getResource("classpath:dataset-es-settings.json"),
              indexName);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @Bean(name = "esOccurrenceClientConfig")
  public EsClient.EsClientConfiguration occurrenceRestHighLevelClient(
      EsManageServer esManageServer) {
    EsClient.EsClientConfiguration esClientConfiguration = new EsClient.EsClientConfiguration();
    esClientConfiguration.setSocketTimeOut(6000);
    esClientConfiguration.setConnectionRequestTimeOut(6000);
    esClientConfiguration.setConnectionRequestTimeOut(6000);
    esClientConfiguration.setHosts(esManageServer.getServerAddress());
    return esClientConfiguration;
  }

  @Bean(name = "registryEsClientConfig")
  @Primary
  public EsClient.EsClientConfiguration registryRestHighLevelClient(EsManageServer esManageServer) {
    return occurrenceRestHighLevelClient(esManageServer);
  }

  @Bean
  @Primary
  public RestHighLevelClient restHighLevelClient(
      @Qualifier("registryEsClientConfig") EsClient.EsClientConfiguration esClientConfiguration,
      @Value("${elasticsearch.mock}") boolean mockEs) {
    return mockEs
        ? Mockito.mock(RestHighLevelClient.class)
        : EsClient.provideEsClient(esClientConfiguration);
  }

  @Bean(name = "occurrenceEsClient")
  public RestHighLevelClient occurrenceRestHighLevelClient(
      @Qualifier("esOccurrenceClientConfig") EsClient.EsClientConfiguration esClientConfiguration,
      @Value("${elasticsearch.mock}") boolean mockEs) {
    return mockEs
        ? Mockito.mock(RestHighLevelClient.class)
        : EsClient.provideEsClient(esClientConfiguration);
  }
}
