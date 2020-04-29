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

import java.nio.file.Paths;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DatasetElasticsearchConfiguration {

  @Bean("datasetElasticCluster")
  public EsManageServer esManageServer() {
    try {
      return new EsManageServer(
          Paths.get(
              DatasetElasticsearchConfiguration.class
                  .getClassLoader()
                  .getResource("dataset-es-mapping.json")
                  .getPath()),
          "dataset",
          "dataset");
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @Bean(name = "esOccurrenceClientConfig")
  public EsClient.EsClientConfiguration occurrenceRestHighLevelClient(
      EsManageServer esManageServer) {
    EsClient.EsClientConfiguration esClientConfiguration = new EsClient.EsClientConfiguration();
    esClientConfiguration.setMaxRetryTimeOut(6000);
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
}
