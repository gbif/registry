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
package org.gbif.registry.search.test;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;

import org.gbif.registry.search.dataset.indexing.es.EsClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;

/**
 * Test-specific Elasticsearch configuration that provides the same beans as EsConfiguration
 * but uses ElasticsearchTestContainerConfiguration for the underlying Elasticsearch instance.
 * This configuration overrides the production EsConfiguration beans for testing purposes.
 */
@Configuration
public class DatasetElasticsearchConfiguration {

  @Autowired private ResourceLoader resourceLoader;

  @Bean("elasticsearchTestContainerConfiguration")
  @Scope("singleton")
  public ElasticsearchTestContainerConfiguration elasticsearchTestContainer(
      @Value("${elasticsearch.registry.index}") String indexName) {
    try {
      return new ElasticsearchTestContainerConfiguration(
          resourceLoader,
          indexName,
          "classpath:dataset-es-mapping.json",
          "classpath:dataset-es-settings.json");
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @Bean(name = "esOccurrenceClientConfig")
  public EsClient.EsClientConfiguration occurrenceEsClientConfiguration(
      @Qualifier("elasticsearchTestContainerConfiguration") ElasticsearchTestContainerConfiguration elasticsearchTestContainer) {
    EsClient.EsClientConfiguration esClientConfiguration = new EsClient.EsClientConfiguration();
    esClientConfiguration.setConnectionTimeOut(6000);
    esClientConfiguration.setConnectionRequestTimeOut(6000);
    esClientConfiguration.setSocketTimeOut(6000);
    // Use testcontainer address instead of production config
    esClientConfiguration.setHosts(elasticsearchTestContainer.getServerAddress());
    return esClientConfiguration;
  }

  @Bean(name = "registryEsClientConfig")
  @Primary
  public EsClient.EsClientConfiguration registryEsClientConfiguration(
      @Qualifier("elasticsearchTestContainerConfiguration") ElasticsearchTestContainerConfiguration elasticsearchTestContainer) {
    return occurrenceEsClientConfiguration(elasticsearchTestContainer);
  }

  @Bean
  @Primary
  public ElasticsearchClient elasticsearchClient(
      @Qualifier("registryEsClientConfig") EsClient.EsClientConfiguration esClientConfiguration) {
    return EsClient.provideElasticsearchClient(esClientConfiguration);
  }

  @Bean
  @Primary
  public ElasticsearchAsyncClient elasticsearchAsyncClient(
      @Qualifier("registryEsClientConfig") EsClient.EsClientConfiguration esClientConfiguration) {
    return EsClient.provideElasticsearchAsyncClient(esClientConfiguration);
  }

  @Bean(name = "occurrenceEsClient")
  public ElasticsearchClient occurrenceElasticsearchClient(
      @Qualifier("esOccurrenceClientConfig") EsClient.EsClientConfiguration esClientConfiguration) {
    return EsClient.provideElasticsearchClient(esClientConfiguration);
  }
}
