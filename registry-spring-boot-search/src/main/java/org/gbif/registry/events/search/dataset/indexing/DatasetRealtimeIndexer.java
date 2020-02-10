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
package org.gbif.registry.events.search.dataset.indexing;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.util.iterables.Iterables;
import org.gbif.registry.events.search.dataset.indexing.es.IndexingConstants;
import org.gbif.registry.events.search.dataset.indexing.ws.GbifWsClient;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DatasetRealtimeIndexer {

  private final RestHighLevelClient restHighLevelClient;

  private final DatasetJsonConverter datasetJsonConverter;

  private final GbifWsClient gbifWsClient;

  @Autowired
  public DatasetRealtimeIndexer(
      RestHighLevelClient restHighLevelClient,
      DatasetJsonConverter datasetJsonConverter,
      GbifWsClient gbifWsClient) {
    this.restHighLevelClient = restHighLevelClient;
    this.datasetJsonConverter = datasetJsonConverter;
    this.gbifWsClient = gbifWsClient;
  }

  private IndexRequest toIndexRequest(Dataset dataset) {
    return new IndexRequest()
        .id(dataset.getKey().toString())
        .index(IndexingConstants.ALIAS)
        .type(IndexingConstants.DATASET_RECORD_TYPE)
        .opType(DocWriteRequest.OpType.INDEX)
        .source(datasetJsonConverter.convert(dataset));
  }

  public void index(Dataset dataset) {
    restHighLevelClient.indexAsync(
        toIndexRequest(dataset),
        RequestOptions.DEFAULT,
        new ActionListener<IndexResponse>() {
          @Override
          public void onResponse(IndexResponse indexResponse) {
            log.info("Dataset indexed {}, result {}", dataset, indexResponse);
          }

          @Override
          public void onFailure(Exception ex) {
            log.error("Error indexing dataset {}", dataset, ex);
          }
        });
  }

  public void index(Iterable<Dataset> datasets) {
    BulkRequest bulkRequest = new BulkRequest();
    datasets.forEach(dataset -> bulkRequest.add(toIndexRequest(dataset)));
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
          }

          @Override
          public void onFailure(Exception e) {
            log.error("Error indexing datasets", e);
          }
        });
  }

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

  public void index(Installation installation) {
    // first purge cache
    gbifWsClient.purge(installation);

    // Update hosted datasets for the organization
    try {
      log.debug("Updating hosted datasets for installation {}", installation.getKey());
      Iterable<Dataset> datasets =
          Iterables.datasetsIterable(
              page -> gbifWsClient.getInstallationDatasets(installation.getKey().toString()));
      index(datasets);
    } catch (Exception e) {
      log.error(
          "Unable to update hosted datasets for installation {} - index is now out of sync",
          installation.getKey(),
          e);
    }
  }

  public void delete(Dataset dataset) {
    DeleteRequest deleteRequest =
        new DeleteRequest()
            .id(dataset.getKey().toString())
            .index(IndexingConstants.ALIAS)
            .type(IndexingConstants.DATASET_RECORD_TYPE);

    restHighLevelClient.deleteAsync(
        deleteRequest,
        RequestOptions.DEFAULT,
        new ActionListener<DeleteResponse>() {
          @Override
          public void onResponse(DeleteResponse deleteResponse) {
            log.info("Dataset deleted {}, result {}", dataset, deleteResponse);
          }

          @Override
          public void onFailure(Exception ex) {
            log.error("Error deleting dataset {}", dataset, ex);
          }
        });
  }
}
