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
package org.gbif.registry.guice;

import org.gbif.registry.oaipmh.OaipmhItemRepository;
import org.gbif.registry.oaipmh.OaipmhSetRepository;
import org.gbif.registry.occurrenceclient.OccurrenceMetricsClient;

import java.util.UUID;

import org.dspace.xoai.dataprovider.repository.ItemRepository;
import org.dspace.xoai.dataprovider.repository.RepositoryConfiguration;
import org.dspace.xoai.dataprovider.repository.SetRepository;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

/** Mock class that is mainly used to mock the CubeService. */
public class OaipmhMockModule extends AbstractModule {

  private RepositoryConfiguration repositoryConfiguration;

  public OaipmhMockModule(RepositoryConfiguration repositoryConfiguration) {
    this.repositoryConfiguration = repositoryConfiguration;
  }

  @Override
  protected void configure() {
    bind(RepositoryConfiguration.class).toInstance(repositoryConfiguration);
    bind(ItemRepository.class).to(OaipmhItemRepository.class).in(Scopes.SINGLETON);
    bind(SetRepository.class).to(OaipmhSetRepository.class).in(Scopes.SINGLETON);
    bind(OccurrenceMetricsClient.class).to(MockOccurrenceMetricsClient.class).in(Scopes.SINGLETON);
  }

  private static class MockOccurrenceMetricsClient implements OccurrenceMetricsClient {
    @Override
    public Long getCountForDataset(UUID datasetKey) throws IllegalArgumentException {
      return 0l;
    }
  }
}
