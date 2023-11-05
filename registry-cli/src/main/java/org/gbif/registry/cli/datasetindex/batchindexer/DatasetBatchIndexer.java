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
package org.gbif.registry.cli.datasetindex.batchindexer;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.registry.search.dataset.indexing.DatasetJsonConverter;
import org.gbif.registry.search.dataset.indexing.es.EsClient;
import org.gbif.registry.search.dataset.indexing.es.IndexingConstants;
import org.gbif.registry.search.dataset.indexing.ws.GbifWsClient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;

import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import lombok.extern.slf4j.Slf4j;

/** A builder that will clear and build a new dataset index by paging over the given service. */
@Slf4j
public class DatasetBatchIndexer {

  private final GbifWsClient gbifWsClient;

  private final EsClient esClient;

  private final DatasetJsonConverter datasetJsonConverter;

  private final Integer pageSize;

  // Stops indexing datasets after this amount of records
  // This variable has the only intention to be used in IT and local tests
  private final Integer stopAfter;

  @Autowired
  public DatasetBatchIndexer(
      GbifWsClient gbifWsClient,
      EsClient esClient,
      DatasetJsonConverter datasetJsonConverter,
      @Value("${indexing.stopAfter:-1}") Integer stopAfter,
      @Value("${indexing.pageSize:50}") Integer pageSize) {
    this.gbifWsClient = gbifWsClient;
    this.esClient = esClient;
    this.datasetJsonConverter = datasetJsonConverter;
    this.stopAfter = stopAfter;
    this.pageSize = pageSize;
  }

  /** Pages over all datasets and adds them to ElasticSearch. */
  public void run(DatasetBatchIndexerConfiguration config) {
    log.info("Building a new Dataset index");

    String indexName =
        config.getDatasetEs().getIndex() != null
            ? config.getDatasetEs().getIndex()
            : "dataset_" + new Date().getTime();
    Stopwatch stopwatch = Stopwatch.createStarted();
    esClient.createIndex(
        indexName,
        config.getIndexingSettings(),
        IndexingConstants.MAPPING_FILE,
        IndexingConstants.SETTINGS_FILE);

    ExecutorService executor = Executors.newWorkStealingPool();

    List<CompletableFuture<BulkResponse>> jobs = new ArrayList<>();

    onAllDatasets(
        gbifWsClient,
        pagingResponse ->
            jobs.add(
                CompletableFuture.supplyAsync(
                    () -> index(pagingResponse, datasetJsonConverter, indexName, esClient),
                    executor)),
        stopAfter,
        pageSize);

    CompletableFuture.allOf(jobs.toArray(new CompletableFuture[] {}));

    logIndexingErrors(jobs);
    executor.shutdown();
    esClient.updateSettings(indexName, config.getSearchSettings());
    esClient.swapAlias(config.getDatasetEs().getAlias(), indexName);
    esClient.flushIndex(indexName);
    esClient.close();
    log.info("Finished building Dataset index in {} secs", stopwatch.elapsed(TimeUnit.SECONDS));
  }

  private BulkResponse index(
      PagingResponse<Dataset> pagingResponse,
      DatasetJsonConverter datasetJsonConverter,
      String indexName,
      EsClient esClient) {
    try {
      BulkRequest.Builder bulkRequestBuilder = new BulkRequest.Builder();

      pagingResponse
          .getResults()
          .forEach(
              dataset -> {
                ObjectNode jsonNode = datasetJsonConverter.convert(dataset);
                bulkRequestBuilder.operations(op -> op.index(idx -> idx.index(indexName)
                                                                       .id(dataset.getKey().toString())
                                                                       .document(jsonNode)));
              });
      log.info(
          "Indexing {} datasets until at offset {}",
          pagingResponse.getLimit(),
          pagingResponse.getOffset());
      return esClient.bulk(bulkRequestBuilder.build());
    } catch (Exception ex) {
      log.error("Error indexing page", ex);
      throw new RuntimeException(ex);
    }
  }

  private void onAllDatasets(
      GbifWsClient gbifWsClient,
      Consumer<PagingResponse<Dataset>> responseConsumer,
      int stopAfter,
      int datasetPageSize) {
    int pageSize = stopAfter < 1 ? datasetPageSize : Math.min(datasetPageSize, stopAfter);
    PagingRequest page = new PagingRequest(0, pageSize);
    PagingResponse<Dataset> response = gbifWsClient.listDatasets(new PagingRequest(0, 0));
    int datasetCount = 0;
    do {
      log.debug("Requesting {} datasets starting at offset {}", page.getLimit(), page.getOffset());
      PagingResponse<Dataset> pagingResponse = gbifWsClient.listDatasets(page);
      response.setEndOfRecords(pagingResponse.isEndOfRecords());
      datasetCount += pagingResponse.getResults().size();
      responseConsumer.accept(pagingResponse);
      page.nextPage();
    } while (!response.isEndOfRecords() && (stopAfter < 0 || stopAfter < datasetCount));
  }

  private static void logIndexingErrors(List<CompletableFuture<BulkResponse>> jobs) {
    jobs.forEach(
        job -> {
          try {
            BulkResponse bulkResponse = job.get();
            if (bulkResponse.errors()) {
              log.error("Error in indexing job {}", bulkResponse);
            }
          } catch (InterruptedException | ExecutionException ex) {
            log.error("Error executing job", ex);
          }
        });
  }
}
