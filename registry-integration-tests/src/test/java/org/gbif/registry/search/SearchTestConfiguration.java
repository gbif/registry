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
package org.gbif.registry.search;

import org.gbif.checklistbank.ws.client.DatasetMetricsClient;
import org.gbif.checklistbank.ws.client.SpeciesResourceClient;
import org.gbif.metrics.ws.client.CubeWsClient;
import org.gbif.occurrence.ws.client.OccurrenceWsSearchClient;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SearchTestConfiguration {


  @Bean
  public OccurrenceWsSearchClient occurrenceWsSearchClient() {
    return Mockito.mock(OccurrenceWsSearchClient.class);
  }

  @Bean
  public SpeciesResourceClient speciesResourceClient() {
    return Mockito.mock(SpeciesResourceClient.class);
  }

  @Bean
  public CubeWsClient cubeWsClient() {
    return Mockito.mock(CubeWsClient.class);
  }

  @Bean
  public DatasetMetricsClient datasetMetricsClient() {
    return Mockito.mock(DatasetMetricsClient.class);
  }

}
