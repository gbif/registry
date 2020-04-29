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
package org.gbif.registry.search.dataset.indexing;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.registry.search.dataset.indexing.es.EsConfiguration;
import org.gbif.registry.search.dataset.indexing.es.IndexingConstants;
import org.gbif.registry.search.dataset.indexing.ws.GbifWsClient;

import java.util.concurrent.atomic.AtomicInteger;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@ConditionalOnBean(EsConfiguration.class)
@Slf4j
@Component
public class EsDatasetRealtimeIndexer implements DatasetRealtimeIndexer {

  private final RestHighLevelClient restHighLevelClient;

  private final DatasetJsonConverter datasetJsonConverter;

  private final GbifWsClient gbifWsClient;

  private final AtomicInteger pendingUpdates;

  @Autowired
  public EsDatasetRealtimeIndexer(
      RestHighLevelClient restHighLevelClient,
      DatasetJsonConverter datasetJsonConverter,
      GbifWsClient gbifWsClient) {
    this.restHighLevelClient = restHighLevelClient;
    this.datasetJsonConverter = datasetJsonConverter;
    this.gbifWsClient = gbifWsClient;
    pendingUpdates = new AtomicInteger();
  }

  private IndexRequest toIndexRequest(Dataset dataset) {
    return new IndexRequest()
        .id(dataset.getKey().toString())
        .index(IndexingConstants.ALIAS)
        .type(IndexingConstants.DATASET_RECORD_TYPE)
        .opType(DocWriteRequest.OpType.INDEX)
        .source(datasetJsonConverter.convertAsJsonString(dataset), XContentType.JSON);
  }

  @Override
  public void index(Dataset dataset) {
    pendingUpdates.incrementAndGet();
    try {
      restHighLevelClient.indexAsync(
          toIndexRequest(dataset),
          RequestOptions.DEFAULT,
          new ActionListener<IndexResponse>() {
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
  public void index(Iterable<Dataset> datasets) {
    final AtomicInteger updatesCount = new AtomicInteger();
    BulkRequest bulkRequest = new BulkRequest();

    datasets.forEach(
        dataset -> {
          bulkRequest.add(toIndexRequest(dataset));
          updatesCount.incrementAndGet();
        });
    pendingUpdates.getAndAdd(updatesCount.get());
    try {
      restHighLevelClient.bulkAsync(
          bulkRequest,
          RequestOptions.DEFAULT,
          new ActionListener<BulkResponse>() {
            @Override
            public void onResponse(BulkResponse bulkItemResponses) {
              if (bulkItemResponses.hasFailures()) {
                log.error(
                    "Eror indexing datasets indexed {}", bulkItemResponses.buildFailureMessage());
              } else {
                log.info("Datasets indexed");
              }
              pendingUpdates.addAndGet(-updatesCount.get());
            }

            @Override
            public void onFailure(Exception e) {
              log.error("Error indexing datasets", e);
            }
          });
    } catch (Exception ex) {
      log.error("Error indexing datasets", ex);
      pendingUpdates.addAndGet(-updatesCount.get());
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
    DeleteRequest deleteRequest =
        new DeleteRequest()
            .id(dataset.getKey().toString())
            .index(IndexingConstants.ALIAS)
            .type(IndexingConstants.DATASET_RECORD_TYPE);
    try {
      restHighLevelClient.deleteAsync(
          deleteRequest,
          RequestOptions.DEFAULT,
          new ActionListener<DeleteResponse>() {
            @Override
            public void onResponse(DeleteResponse deleteResponse) {
              log.info("Dataset deleted {}, result {}", dataset.getKey(), deleteResponse);
              pendingUpdates.decrementAndGet();
            }

            @Override
            public void onFailure(Exception ex) {
              log.error("Error deleting dataset {}", dataset, ex);
              pendingUpdates.decrementAndGet();
            }
          });
    } catch (Exception ex) {
      log.error("Error deleting dataset {}", dataset, ex);
      pendingUpdates.decrementAndGet();
    }
  }

  @Override
  public int getPendingUpdates() {
    return pendingUpdates.get();
  }
}
