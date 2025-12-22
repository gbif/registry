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

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.json.JsonData;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EsDatasetRealtimeIndexer implements DatasetRealtimeIndexer {

  private final ElasticsearchAsyncClient elasticsearchAsyncClient;

  private final DatasetJsonConverter datasetJsonConverter;

  private final GbifWsClient gbifWsClient;

  private final AtomicInteger pendingUpdates;

  private final String index;

  private final ElasticsearchClient elasticsearchClient;

  @Autowired
  public EsDatasetRealtimeIndexer(
    ElasticsearchAsyncClient elasticsearchAsyncClient,
    ElasticsearchClient elasticsearchClient,
    DatasetJsonConverter datasetJsonConverter,
    GbifWsClient gbifWsClient,
    @Value("${elasticsearch.registry.index}") String index) {
    this.elasticsearchAsyncClient = elasticsearchAsyncClient;
    this.elasticsearchClient = elasticsearchClient;
    this.datasetJsonConverter = datasetJsonConverter;
    this.gbifWsClient = gbifWsClient;
    this.index = index;
    pendingUpdates = new AtomicInteger();
  }

  private <T> IndexRequest<T> toIndexRequest(Dataset dataset) {
    String jsonString = datasetJsonConverter.convertAsJsonString(dataset);
    return IndexRequest.of(i -> i
      .id(dataset.getKey().toString())
      .index(index)
      .withJson(new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8))));
  }

  @Override
  public void index(Dataset dataset) {
    pendingUpdates.incrementAndGet();
    try {
      elasticsearchAsyncClient.index(toIndexRequest(dataset))
        .thenAccept(indexResponse -> {
          log.info("Dataset indexed {}, result {}", dataset.getKey(), indexResponse);
          // Refresh index to make indexed data searchable immediately
          //refreshIndex();
          pendingUpdates.decrementAndGet();
        })
        .exceptionally(ex -> {
          log.error("Error indexing dataset {}", dataset, ex);
          pendingUpdates.decrementAndGet();
          return null;
        });
    } catch (Exception ex) {
      log.error("Error indexing dataset {}", dataset, ex);
      pendingUpdates.decrementAndGet();
    }
  }

  @Override
  public void index(Iterable<Dataset> datasets) {
    final AtomicInteger updatesCount = new AtomicInteger();
    BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();

    datasets.forEach(
      dataset -> {
        String jsonString = datasetJsonConverter.convertAsJsonString(dataset);
        bulkBuilder.operations(BulkOperation.of(op -> op.index(IndexOperation.of(io -> io
          .id(dataset.getKey().toString())
          .index(index)
          .document(JsonData.fromJson(jsonString))))));
        updatesCount.incrementAndGet();
      });
    if (updatesCount.get() > 0) {
      pendingUpdates.getAndAdd(updatesCount.get());
      try {
        elasticsearchAsyncClient.bulk(bulkBuilder.build())
          .thenAccept(bulkResponse -> {
            if (bulkResponse.errors()) {
              log.error("Error indexing datasets - bulk operation had errors");
              bulkResponse.items().forEach(item -> {
                if (item.error() != null) {
                  log.error("Bulk item error: {}", item.error().reason());
                }
              });
            } else {
              log.info("Datasets indexed successfully");
              // Refresh index to make indexed data searchable immediately
              refreshIndex();
            }
            pendingUpdates.addAndGet(-updatesCount.get());
          })
          .exceptionally(ex -> {
            log.error("Error indexing datasets", ex);
            pendingUpdates.addAndGet(-updatesCount.get());
            return null;
          });
      } catch (Exception ex) {
        log.error("Error indexing datasets", ex);
        pendingUpdates.addAndGet(-updatesCount.get());
      }
    }
  }

  @Override
  public void index(Organization organization) {
    // first purge cache
    gbifWsClient.purge(organization);
    // Update published datasets for the organization
    try {
      log.debug("Updating published datasets for organization {}", organization.getKey());
      Iterable<Dataset> datasets =
        Iterables.datasetsIterable(
          page ->
            gbifWsClient.getOrganizationPublishedDataset(
              organization.getKey().toString(), page));
      index(datasets);
    } catch (Exception e) {
      log.error(
        "Unable to update published datasets for organization {} - index is now out of sync",
        organization.getKey(),
        e);
    }

    // Update hosted datasets for the organization
    try {
      log.debug(
        "Updating hosted datasets for organization {}: {}",
        organization.getKey(),
        organization.getTitle());
      Iterable<Dataset> datasets =
        Iterables.datasetsIterable(
          page ->
            gbifWsClient.getOrganizationHostedDatasets(
              organization.getKey().toString(), page));
      index(datasets);
    } catch (Exception e) {
      log.error(
        "Unable to update hosted datasets for organization {} - index is now out of sync",
        organization.getKey(),
        e);
    }
  }

  @Override
  public void index(Installation installation) {
    // first purge cache
    gbifWsClient.purge(installation);

    // Update hosted datasets for the organization
    try {
      log.debug("Updating hosted datasets for installation {}", installation.getKey());
      Iterable<Dataset> datasets =
        Iterables.datasetsIterable(
          page -> gbifWsClient.getInstallationDatasets(installation.getKey().toString(), page));
      index(datasets);
    } catch (Exception e) {
      log.error(
        "Unable to update hosted datasets for installation {} - index is now out of sync",
        installation.getKey(),
        e);
    }
  }

  @Override
  public void delete(Dataset dataset) {
    pendingUpdates.incrementAndGet();
    DeleteRequest deleteRequest = DeleteRequest.of(d -> d
      .id(dataset.getKey().toString())
      .index(IndexingConstants.ALIAS));
    try {
      elasticsearchAsyncClient.delete(deleteRequest)
        .thenAccept(deleteResponse -> {
          log.info("Dataset deleted {}, result {}", dataset.getKey(), deleteResponse);
          pendingUpdates.decrementAndGet();
        })
        .exceptionally(ex -> {
          log.error("Error deleting dataset {}", dataset, ex);
          pendingUpdates.decrementAndGet();
          return null;
        });
    } catch (Exception ex) {
      log.error("Error deleting dataset {}", dataset, ex);
      pendingUpdates.decrementAndGet();
    }
  }

  @Override
  public void index(Network network) {
    // Update hosted datasets for the organization
    try {
      log.debug("Updating hosted datasets for installation {}", network.getKey());
      Iterable<Dataset> datasets =
        Iterables.datasetsIterable(
          page -> gbifWsClient.getNetworkDatasets(network.getKey().toString(), page));
      index(datasets);
    } catch (Exception e) {
      log.error(
        "Unable to update hosted datasets for network {} - index is now out of sync",
        network.getKey(),
        e);
    }
  }

  @Override
  public int getPendingUpdates() {
    return pendingUpdates.get();
  }

  private void refreshIndex() {
    try {
      elasticsearchClient.indices().refresh(r -> r.index(index));
    } catch (Exception ex) {
      log.warn("Failed to refresh index after indexing", ex);
    }
  }
}
