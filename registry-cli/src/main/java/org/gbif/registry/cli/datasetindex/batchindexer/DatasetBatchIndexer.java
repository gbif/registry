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

import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.json.JsonData;

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

import com.google.common.base.Stopwatch;

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
      @Value("${indexing.pageSize:20}") Integer pageSize) {
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

    // Use fixed thread pool with limited concurrency to avoid overwhelming ES
    ExecutorService executor = Executors.newFixedThreadPool(4);

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

    CompletableFuture.allOf(jobs.toArray(new CompletableFuture[] {})).join();

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
    // Pre-convert all datasets to JSON strings (this is the expensive part)
    List<String> jsonStrings = new ArrayList<>();
    List<String> datasetIds = new ArrayList<>();
    pagingResponse
        .getResults()
        .forEach(
            dataset -> {
              jsonStrings.add(datasetJsonConverter.convertAsJsonString(dataset));
              datasetIds.add(dataset.getKey().toString());
            });

    log.info(
        "Indexing {} datasets until at offset {}",
        pagingResponse.getLimit(),
        pagingResponse.getOffset());

    // Retry with exponential backoff for rejected execution errors
    int maxRetries = 5;
    int retryCount = 0;
    long waitTime = 1000; // Start with 1 second

    while (true) {
      try {
        // Build a fresh BulkRequest for each attempt (builders can only be used once)
        BulkRequest.Builder bulkRequest = new BulkRequest.Builder();
        for (int i = 0; i < jsonStrings.size(); i++) {
          final String jsonString = jsonStrings.get(i);
          final String datasetId = datasetIds.get(i);
          bulkRequest.operations(BulkOperation.of(op -> op.index(IndexOperation.of(io -> io
              .index(indexName)
              .id(datasetId)
              .document(JsonData.fromJson(jsonString))))));
        }
        BulkResponse response = esClient.bulk(bulkRequest.build());

        // Check if any items failed due to rejected execution
        if (response.errors()) {
          boolean hasRejectedExecution = response.items().stream()
              .anyMatch(item -> item.error() != null &&
                  item.error().type() != null &&
                  item.error().type().contains("rejected_execution"));

          if (hasRejectedExecution && retryCount < maxRetries) {
            retryCount++;
            log.warn("Bulk request rejected due to ES backpressure, retrying in {}ms (attempt {}/{})",
                waitTime, retryCount, maxRetries);
            Thread.sleep(waitTime);
            waitTime *= 2; // Exponential backoff
            continue;
          }
        }
        return response;
      } catch (Exception ex) {
        if (retryCount < maxRetries && ex.getMessage() != null &&
            ex.getMessage().contains("rejected_execution")) {
          retryCount++;
          log.warn("Bulk request rejected, retrying in {}ms (attempt {}/{})",
              waitTime, retryCount, maxRetries);
          try {
            Thread.sleep(waitTime);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during retry wait", ie);
          }
          waitTime *= 2;
          continue;
        }
        log.error("Error indexing page", ex);
        throw new RuntimeException(ex);
      }
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
    jobs.forEach(job -> {
      try {
        BulkResponse bulkResponse = job.get();
        if (bulkResponse.errors()) {
          for (BulkResponseItem itemResponse : bulkResponse.items()) {
            if (itemResponse.error() != null) {
              log.error(
                "Indexing failure: index={}, id={}, error={}",
                itemResponse.index(),
                itemResponse.id(),
                itemResponse.error()
              );
            }
          }
        }
      } catch (InterruptedException | ExecutionException e) {
        log.error("Failed to get bulk indexing response", e);
        Thread.currentThread().interrupt(); // Best practice for InterruptedException
      }
    });
  }

}
