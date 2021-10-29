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
package org.gbif.registry.search.dataset.indexing.es;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class EsConfiguration {

  @ConfigurationProperties(prefix = "elasticsearch.registry")
  @Bean("registryEsClientConfig")
  @ConditionalOnProperty("elasticsearch.registry.enabled")
  @Primary
  public EsClient.EsClientConfiguration registryEsClientConfiguration() {
    return new EsClient.EsClientConfiguration();
  }

  @Bean
  @Primary
  public RestHighLevelClient restHighLevelClient(
      @Qualifier("registryEsClientConfig") EsClient.EsClientConfiguration esClientConfiguration) {
    return EsClient.provideEsClient(esClientConfiguration);
  }

  @ConfigurationProperties(prefix = "elasticsearch.occurrence")
  @Bean("esOccurrenceClientConfig")
  @ConditionalOnProperty("elasticsearch.occurrence.enabled")
  public EsClient.EsClientConfiguration occurrenceEsClientConfiguration() {
    return new EsClient.EsClientConfiguration();
  }

  @Bean(name = "occurrenceEsClient")
  public RestHighLevelClient occurrenceRestHighLevelClient(
      @Qualifier("esOccurrenceClientConfig") EsClient.EsClientConfiguration esClientConfiguration) {
    return EsClient.provideEsClient(esClientConfiguration);
  }
}
