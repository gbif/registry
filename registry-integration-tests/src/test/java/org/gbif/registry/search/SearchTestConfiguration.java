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

import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.checklistbank.ws.client.DatasetMetricsClient;
import org.gbif.checklistbank.ws.client.SpeciesResourceClient;
import org.gbif.metrics.ws.client.CubeWsClient;
import org.gbif.occurrence.ws.client.OccurrenceWsSearchClient;
import org.gbif.registry.search.dataset.indexing.ws.GbifWsClient;
import org.gbif.registry.search.dataset.indexing.ws.GbifWsWrapperClient;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration(proxyBeanMethods = false)
public class SearchTestConfiguration {

  @Bean
  @Primary
  public GbifWsClient gbifWsClient(
      InstallationService installationService,
      OrganizationService organizationService,
      DatasetService datasetService,
      NetworkService networkService) {
      return new GbifWsWrapperClient(installationService,
                                      organizationService,
                                      datasetService,
                                      networkService,
                                      Mockito.mock(OccurrenceWsSearchClient.class),
                                      Mockito.mock(SpeciesResourceClient.class),
                                      Mockito.mock(CubeWsClient.class),
                                      Mockito.mock(DatasetMetricsClient.class));
  }

}
