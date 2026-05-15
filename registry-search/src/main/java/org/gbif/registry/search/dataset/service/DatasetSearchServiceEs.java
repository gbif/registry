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
package org.gbif.registry.search.dataset.service;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.model.registry.search.DatasetSearchRequest;
import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.model.registry.search.DatasetSuggestRequest;
import org.gbif.api.model.registry.search.DatasetSuggestResult;
import org.gbif.api.service.registry.DatasetSearchService;
import org.gbif.registry.search.dataset.DatasetEsFieldMapper;
import org.gbif.registry.search.dataset.DatasetEsResponseParser;
import org.gbif.registry.search.dataset.common.EsSearchRequestBuilder;

import java.util.List;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import java.util.concurrent.CompletableFuture;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Qualifier("datasetSearchServiceEs")

public class DatasetSearchServiceEs implements DatasetSearchService, AsyncDatasetSearchService {

  private static final int DEFAULT_SUGGEST_LIMIT = 10;
  private static final int MAX_SUGGEST_LIMIT = 100;

  private final DatasetEsResponseParser esResponseParser = DatasetEsResponseParser.create();
  private final ElasticsearchClient elasticsearchClient;
  private final String index;

  private final EsSearchRequestBuilder<DatasetSearchParameter> esSearchRequestBuilder =
      new EsSearchRequestBuilder<>(new DatasetEsFieldMapper());

  private final ElasticsearchAsyncClient elasticsearchAsyncClient;

  @Autowired
  public DatasetSearchServiceEs(
      @Value("${elasticsearch.registry.index}") String index,
      ElasticsearchClient elasticsearchClient,
      // async client is optional in some configurations - Spring will inject if available
      ElasticsearchAsyncClient elasticsearchAsyncClient) {
    this.index = index;
    this.elasticsearchClient = elasticsearchClient;
    this.elasticsearchAsyncClient = elasticsearchAsyncClient;
  }

  @Override
  public SearchResponse<DatasetSearchResult, DatasetSearchParameter> search(
      DatasetSearchRequest datasetSearchRequest) {
    try {
      SearchRequest searchRequest =
          esSearchRequestBuilder.buildSearchRequest(datasetSearchRequest, true, index);
      log.debug("Search request: {}", searchRequest);
      co.elastic.clients.elasticsearch.core.SearchResponse<ObjectNode> response =
          elasticsearchClient.search(searchRequest, ObjectNode.class);
      return esResponseParser.buildSearchResponse(response, datasetSearchRequest);
    } catch (Exception ex) {
      // If the thread was interrupted while waiting on the low-level future, restore the
      // interrupt flag and provide a clearer error.
      Throwable cause = ex instanceof RuntimeException ? ex.getCause() : ex;
      if (cause instanceof InterruptedException) {
        Thread.currentThread().interrupt();
        log.warn("Search was interrupted", ex);
        throw new RuntimeException("Search was interrupted", ex);
      }

      log.error("Error while searching datasets: {} - {}", ex.getClass().getName(), ex.getMessage());
      Throwable cause2 = ex.getCause();
      int depth2 = 0;
      while (cause2 != null && depth2 < 10) {
        log.error("cause[{}]: {} - {}", depth2, cause2.getClass().getName(), cause2.getMessage());
        cause2 = cause2.getCause();
        depth2++;
      }
      throw new RuntimeException(ex);
    }
  }

  /**
   * Asynchronous version of search that returns a CompletableFuture. Useful to avoid blocking
   * request threads and to surface more detailed errors upstream.
   */
  public CompletableFuture<SearchResponse<DatasetSearchResult, DatasetSearchParameter>> searchAsync(
      DatasetSearchRequest datasetSearchRequest) {
    if (elasticsearchAsyncClient == null) {
      return CompletableFuture.failedFuture(new IllegalStateException("Elasticsearch async client not configured"));
    }

    try {
      SearchRequest searchRequest =
          esSearchRequestBuilder.buildSearchRequest(datasetSearchRequest, true, index);
      log.debug("Async search request: {}", searchRequest);

      CompletableFuture<co.elastic.clients.elasticsearch.core.SearchResponse<ObjectNode>> esFuture =
          elasticsearchAsyncClient.search(searchRequest, ObjectNode.class);

      return esFuture.thenApply(response -> esResponseParser.buildSearchResponse(response, datasetSearchRequest))
          .exceptionally(ex -> {
            // If interrupted, restore flag
            Throwable cause = ex instanceof RuntimeException ? ex.getCause() : ex;
            if (cause instanceof InterruptedException) {
              Thread.currentThread().interrupt();
            }
            log.error("Async search failed: {} - {}", ex.getClass().getName(), ex.getMessage());
            Throwable nested = ex.getCause();
            int d = 0;
            while (nested != null && d < 10) {
              log.error("cause[{}]: {} - {}", d, nested.getClass().getName(), nested.getMessage());
              nested = nested.getCause();
              d++;
            }
            throw new RuntimeException("Async search failed", ex);
          });
    } catch (Exception ex) {
      return CompletableFuture.failedFuture(ex);
    }
  }

  /**
   * Asynchronous version of suggest that returns a CompletableFuture of the suggestion results.
   */
  public CompletableFuture<java.util.List<DatasetSuggestResult>> suggestAsync(
      DatasetSuggestRequest datasetSuggestRequest) {
    if (elasticsearchAsyncClient == null) {
      return CompletableFuture.failedFuture(new IllegalStateException("Elasticsearch async client not configured"));
    }

    try {
      int limit = datasetSuggestRequest.getLimit();
      if (limit <= 0) {
        limit = DEFAULT_SUGGEST_LIMIT;
      } else if (limit > MAX_SUGGEST_LIMIT) {
        limit = MAX_SUGGEST_LIMIT;
      }

      // Create a copy of the request with the validated limit
      DatasetSuggestRequest modifiedRequest = new DatasetSuggestRequest();
      modifiedRequest.setQ(datasetSuggestRequest.getQ());
      modifiedRequest.setLimit(limit);
      modifiedRequest.setOffset(datasetSuggestRequest.getOffset());
      modifiedRequest.setParameters(datasetSuggestRequest.getParameters());

      SearchRequest searchRequest =
          esSearchRequestBuilder.buildAutocompleteQuery(modifiedRequest, DatasetSearchParameter.DATASET_TITLE, index);
      log.debug("Async suggest request: {}", searchRequest);

      CompletableFuture<co.elastic.clients.elasticsearch.core.SearchResponse<ObjectNode>> esFuture =
          elasticsearchAsyncClient.search(searchRequest, ObjectNode.class);

      return esFuture.thenApply(response -> {
        org.gbif.api.model.common.search.SearchResponse<DatasetSuggestResult, org.gbif.api.model.registry.search.DatasetSearchParameter> autocompleteResponse =
            esResponseParser.buildSearchAutocompleteResponse(response, modifiedRequest);
        return autocompleteResponse.getResults();
      }).exceptionally(ex -> {
        Throwable cause = ex instanceof RuntimeException ? ex.getCause() : ex;
        if (cause instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
        log.error("Async suggest failed: {} - {}", ex.getClass().getName(), ex.getMessage());
        Throwable nested = ex.getCause();
        int d = 0;
        while (nested != null && d < 10) {
          log.error("cause[{}]: {} - {}", d, nested.getClass().getName(), nested.getMessage());
          nested = nested.getCause();
          d++;
        }
        throw new RuntimeException("Async suggest failed", ex);
      });
    } catch (Exception ex) {
      return CompletableFuture.failedFuture(ex);
    }
  }

  @Override
  public List<DatasetSuggestResult> suggest(DatasetSuggestRequest datasetSuggestRequest) {
    try {
      int limit = datasetSuggestRequest.getLimit();
      if (limit <= 0) {
        limit = DEFAULT_SUGGEST_LIMIT;
      } else if (limit > MAX_SUGGEST_LIMIT) {
        limit = MAX_SUGGEST_LIMIT;
      }

      // Create a copy of the request with the validated limit
      DatasetSuggestRequest modifiedRequest = new DatasetSuggestRequest();
      modifiedRequest.setQ(datasetSuggestRequest.getQ());
      modifiedRequest.setLimit(limit);
      modifiedRequest.setOffset(datasetSuggestRequest.getOffset());
      modifiedRequest.setParameters(datasetSuggestRequest.getParameters());

      SearchRequest searchRequest =
          esSearchRequestBuilder.buildAutocompleteQuery(modifiedRequest, DatasetSearchParameter.DATASET_TITLE, index);
      log.debug("Search suggest request: {}", searchRequest);
      co.elastic.clients.elasticsearch.core.SearchResponse<ObjectNode> response =
          elasticsearchClient.search(searchRequest, ObjectNode.class);
      org.gbif.api.model.common.search.SearchResponse<DatasetSuggestResult, org.gbif.api.model.registry.search.DatasetSearchParameter> autocompleteResponse =
          esResponseParser.buildSearchAutocompleteResponse(response, modifiedRequest);
      return autocompleteResponse.getResults();
    } catch (Exception ex) {
      log.error("Error executing the search operation", ex);
      throw new RuntimeException(ex);
    }
  }
}
