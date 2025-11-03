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
package org.gbif.registry.search.dataset.indexing;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.registry.search.dataset.indexing.es.IndexingConstants;
import org.gbif.registry.search.dataset.indexing.ws.GbifWsClient;

import java.util.concurrent.atomic.AtomicInteger;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EsDatasetRealtimeIndexer implements DatasetRealtimeIndexer {

  private final ElasticsearchClient elasticsearchClient;

  private final DatasetJsonConverter datasetJsonConverter;

  private final GbifWsClient gbifWsClient;

  private final AtomicInteger pendingUpdates;

  private final String index;

  @Autowired
  public EsDatasetRealtimeIndexer(
      ElasticsearchClient elasticsearchClient,
      DatasetJsonConverter datasetJsonConverter,
      GbifWsClient gbifWsClient,
      @Value("${elasticsearch.registry.index}") String index) {
    this.elasticsearchClient = elasticsearchClient;
    this.datasetJsonConverter = datasetJsonConverter;
    this.gbifWsClient = gbifWsClient;
    this.index = index;
    pendingUpdates = new AtomicInteger();
  }

  private IndexRequest<Object> toIndexRequest(Dataset dataset) {
    return new IndexRequest.Builder<Object>()
        .id(dataset.getKey().toString())
        .index(index)
        .document(datasetJsonConverter.convertAsJsonString(dataset))
        .build();
  }

  @Override
  public void index(Dataset dataset) {
    pendingUpdates.incrementAndGet();
    try {
      elasticsearchClient.indexAsync(
          toIndexRequest(dataset),
          new co.elastic.clients.elasticsearch.ElasticsearchAsyncClient.Listener<IndexResponse>() {
            @Override
            public void onResponse(IndexResponse indexResponse) {
              log.info("Dataset indexed {}, result {}", dataset.getKey(), indexResponse);
              pendingUpdates.decrementAndGet();
            }

            @Override
            public void onFailure(Exception ex) {
              log.error("Error indexing dataset {}", dataset, ex);
              pendingUpdates.decrementAndGet();
            }
          });
    } catch (Exception ex) {
      log.error("Error indexing dataset {}", dataset, ex);
      pendingUpdates.decrementAndGet();
    }
  }

  @Override
  public void index(Organization organization) {
    // Not implemented for real-time indexing

    // first purge dataset and then gbifWsClient.getOrganizationPublishedDataset and hosted  which requires
    //Iterable<Dataset> datasets index
  }

  @Override
  public void index(Installation installation) {
    // Not implemented for real-time indexing
    //getInstallationDatasets and index
  }

  @Override
  public void index(Network network) {
    // Not implemented for real-time indexing
    //gbifWsClient.getNetworkDatasets(
  }

  @Override
  public void deleteDataset(String datasetKey) {
    pendingUpdates.incrementAndGet();
    try {
      DeleteRequest deleteRequest = new DeleteRequest.Builder()
          .id(datasetKey)
          .index(index)
          .build();

      elasticsearchClient.deleteAsync(
          deleteRequest,
          new co.elastic.clients.elasticsearch.ElasticsearchAsyncClient.Listener<DeleteResponse>() {
            @Override
            public void onResponse(DeleteResponse deleteResponse) {
              log.info("Dataset deleted {}, result {}", datasetKey, deleteResponse);
              pendingUpdates.decrementAndGet();
            }

            @Override
            public void onFailure(Exception ex) {
              log.error("Error deleting dataset {}", datasetKey, ex);
              pendingUpdates.decrementAndGet();
            }
          });
    } catch (Exception ex) {
      log.error("Error deleting dataset {}", datasetKey, ex);
      pendingUpdates.decrementAndGet();
    }
  }

  @Override
  public void deleteOrganization(String organizationKey) {
    // Not implemented for real-time indexing
  }

  @Override
  public void deleteInstallation(String installationKey) {
    // Not implemented for real-time indexing
  }

  @Override
  public void deleteNetwork(String networkKey) {
    // Not implemented for real-time indexing
  }

  @Override
  public int getPendingUpdates() {
    return pendingUpdates.get();
  }

  @Override
  public void indexAllDatasets() {
    log.info("Starting full dataset indexing");
    for (Dataset dataset : Iterables.datasets(gbifWsClient)) {
      index(dataset);
    }
    log.info("Finished full dataset indexing");
  }
}
