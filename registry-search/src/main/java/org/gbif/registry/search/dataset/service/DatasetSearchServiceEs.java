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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ObjectNode;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Qualifier("datasetSearchServiceEs")

public class DatasetSearchServiceEs implements DatasetSearchService, AsyncDatasetSearchService {

  private static final int DEFAULT_SUGGEST_LIMIT = 10;
  private static final int MAX_SUGGEST_LIMIT = 100;

  // Parameters handled by the custom correlated filters below instead of the generic builder.
  private static final Set<DatasetSearchParameter> CUSTOM_FILTER_PARAMETERS =
      Set.of(
          DatasetSearchParameter.MACHINE_TAG_NAMESPACE,
          DatasetSearchParameter.MACHINE_TAG_NAME,
          DatasetSearchParameter.MACHINE_TAG_VALUE,
          DatasetSearchParameter.IDENTIFIER,
          DatasetSearchParameter.IDENTIFIER_TYPE);

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
      SearchRequest searchRequest = buildSearchRequest(datasetSearchRequest);
      log.debug("Search request: {}", searchRequest);
      co.elastic.clients.elasticsearch.core.SearchResponse<ObjectNode> response =
          elasticsearchClient.search(searchRequest, ObjectNode.class);
      return esResponseParser.buildSearchResponse(response, datasetSearchRequest);
    } catch (Exception ex) {
      // If the thread was interrupted while waiting on the low-level future, restore the
      // interrupt flag and provide a clearer error.
      this.handleInterruptedException(ex);
      throw new RuntimeException(ex);
    }
  }

  /**
   * Asynchronous version of search that returns a CompletableFuture. Useful to avoid blocking
   * request threads and to surface more detailed errors upstream.
   */
  @Async("boundedTaskExecutor")
  public CompletableFuture<SearchResponse<DatasetSearchResult, DatasetSearchParameter>> searchAsync(
      DatasetSearchRequest datasetSearchRequest) {
    if (elasticsearchAsyncClient == null) {
      return CompletableFuture.failedFuture(new IllegalStateException("Elasticsearch async client not configured"));
    }

    try {
      SearchRequest searchRequest = buildSearchRequest(datasetSearchRequest);
      log.debug("Async search request: {}", searchRequest);

      CompletableFuture<co.elastic.clients.elasticsearch.core.SearchResponse<ObjectNode>> esFuture =
          elasticsearchAsyncClient.search(searchRequest, ObjectNode.class);

      return esFuture.thenApply(response -> esResponseParser.buildSearchResponse(response, datasetSearchRequest))
        .exceptionally(ex -> {
          this.handleInterruptedException(ex);
          throw new RuntimeException("Async suggest failed", ex);
        });
    } catch (Exception ex) {
      return CompletableFuture.failedFuture(ex);
    }
  }

  /**
   * Asynchronous version of suggest that returns a CompletableFuture of the suggestion results.
   */
  @Async("boundedTaskExecutor")
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
        this.handleInterruptedException(ex);
        throw new RuntimeException("Async suggest failed", ex);
      });
    } catch (Exception ex) {
      return CompletableFuture.failedFuture(ex);
    }
  }

  private SearchRequest buildSearchRequest(DatasetSearchRequest datasetSearchRequest) {
    List<Query> customFilters = new ArrayList<>();
    buildMachineTagFilter(datasetSearchRequest.getParameters()).ifPresent(customFilters::add);
    buildIdentifierFilter(datasetSearchRequest.getParameters()).ifPresent(customFilters::add);
    if (customFilters.isEmpty()) {
      return esSearchRequestBuilder.buildSearchRequest(datasetSearchRequest, true, index);
    }

    // Machine tag and identifier filters need correlated matching across their sub-fields that the
    // generic builder cannot express, so they are stripped here and merged in as bool filters.
    SearchRequest searchRequest =
        esSearchRequestBuilder.buildSearchRequest(
            withoutCustomFilterParameters(datasetSearchRequest), true, index);
    return withFilters(searchRequest, customFilters);
  }

  /**
   * Builds the Elasticsearch filter for machine tag parameters. A single dimension queries the
   * corresponding flat keyword array; combined dimensions query the compound token field so
   * namespace, name and value are matched on the same machine tag.
   */
  private static Optional<Query> buildMachineTagFilter(
      Map<DatasetSearchParameter, Set<String>> parameters) {
    if (parameters == null) {
      return Optional.empty();
    }
    Set<String> namespaces = valuesOf(parameters, DatasetSearchParameter.MACHINE_TAG_NAMESPACE);
    Set<String> names = valuesOf(parameters, DatasetSearchParameter.MACHINE_TAG_NAME);
    Set<String> values = valuesOf(parameters, DatasetSearchParameter.MACHINE_TAG_VALUE);

    int dimensions =
        (namespaces.isEmpty() ? 0 : 1) + (names.isEmpty() ? 0 : 1) + (values.isEmpty() ? 0 : 1);
    if (dimensions == 0) {
      return Optional.empty();
    }
    if (dimensions == 1) {
      if (!namespaces.isEmpty()) {
        return Optional.of(
            termsQuery(DatasetEsFieldMapper.MACHINE_TAG_NAMESPACES_FIELD, namespaces));
      }
      if (!names.isEmpty()) {
        return Optional.of(termsQuery(DatasetEsFieldMapper.MACHINE_TAG_NAMES_FIELD, names));
      }
      return Optional.of(termsQuery(DatasetEsFieldMapper.MACHINE_TAG_VALUES_FIELD, values));
    }

    List<String> tokens = extractTokens(namespaces, names, values);
    return Optional.of(termsQuery(DatasetEsFieldMapper.MACHINE_TAG_TOKENS_FIELD, tokens));
  }

  private static List<String> extractTokens(Set<String> namespaces, Set<String> names,
      Set<String> values) {
    Collection<String> namespaceValues = namespaces.isEmpty() ? Collections.singleton(null) : namespaces;
    Collection<String> nameValues = names.isEmpty() ? Collections.singleton(null) : names;
    Collection<String> valueValues = values.isEmpty() ? Collections.singleton(null) : values;

    List<String> tokens = new ArrayList<>();
    for (String namespace : namespaceValues) {
      for (String name : nameValues) {
        for (String value : valueValues) {
          tokens.add(DatasetEsFieldMapper.machineTagToken(namespace, name, value));
        }
      }
    }
    return tokens;
  }

  /**
   * Builds the Elasticsearch filter for identifier parameters. A single dimension queries the
   * corresponding flat keyword array; type and identifier combined query the compound token field
   * so both are matched on the same identifier.
   */
  private static Optional<Query> buildIdentifierFilter(
      Map<DatasetSearchParameter, Set<String>> parameters) {
    if (parameters == null) {
      return Optional.empty();
    }
    Set<String> types = valuesOf(parameters, DatasetSearchParameter.IDENTIFIER_TYPE);
    Set<String> values = valuesOf(parameters, DatasetSearchParameter.IDENTIFIER);

    if (types.isEmpty() && values.isEmpty()) {
      return Optional.empty();
    }
    if (values.isEmpty()) {
      return Optional.of(termsQuery(DatasetEsFieldMapper.IDENTIFIER_TYPES_FIELD, types));
    }
    if (types.isEmpty()) {
      return Optional.of(termsQuery(DatasetEsFieldMapper.IDENTIFIER_VALUES_FIELD, values));
    }

    List<String> tokens = new ArrayList<>();
    for (String type : types) {
      for (String value : values) {
        tokens.add(DatasetEsFieldMapper.identifierToken(type, value));
      }
    }
    return Optional.of(termsQuery(DatasetEsFieldMapper.IDENTIFIER_TOKENS_FIELD, tokens));
  }

  private static Set<String> valuesOf(
      Map<DatasetSearchParameter, Set<String>> parameters, DatasetSearchParameter parameter) {
    Set<String> values = parameters.get(parameter);
    return values != null ? values : Set.of();
  }

  private static Query termsQuery(String field, Collection<String> values) {
    List<FieldValue> fieldValues = values.stream().map(FieldValue::of).toList();
    if (fieldValues.size() == 1) {
      return Query.of(q -> q.term(t -> t.field(field).value(fieldValues.get(0))));
    }
    return Query.of(q -> q.terms(t -> t.field(field).terms(ts -> ts.value(fieldValues))));
  }

  /** Copies the request without custom filter parameters, which the generic builder cannot map. */
  private static DatasetSearchRequest withoutCustomFilterParameters(DatasetSearchRequest request) {
    DatasetSearchRequest copy = new DatasetSearchRequest(request.getOffset(), request.getLimit());
    copy.setQ(request.getQ());
    copy.setHighlight(request.isHighlight());
    copy.setHighlightContext(request.getHighlightContext());
    copy.setFacetMultiSelect(request.isFacetMultiSelect());
    copy.setFacetMinCount(request.getFacetMinCount());
    copy.setFacetLimit(request.getFacetLimit());
    copy.setFacetOffset(request.getFacetOffset());
    if (request.getFacets() != null) {
      copy.setFacets(new HashSet<>(request.getFacets()));
    }
    if (request.getFacetPages() != null) {
      copy.setFacetPages(new EnumMap<>(request.getFacetPages()));
    }
    if (request.getParameters() != null) {
      Map<DatasetSearchParameter, Set<String>> parameters =
          new EnumMap<>(DatasetSearchParameter.class);
      request
          .getParameters()
          .forEach(
              (key, value) -> {
                if (!CUSTOM_FILTER_PARAMETERS.contains(key)) {
                  parameters.put(key, new HashSet<>(value));
                }
              });
      copy.setParameters(parameters);
    }
    return copy;
  }

  /** Rebuilds the request with the extra filters added to a bool query around the original one. */
  private static SearchRequest withFilters(SearchRequest searchRequest, List<Query> filters) {
    Query existingQuery = searchRequest.query();
    Query mergedQuery =
        Query.of(
            q ->
                q.bool(
                    b -> {
                      if (existingQuery != null) {
                        b.must(existingQuery);
                      } else {
                        b.must(m -> m.matchAll(ma -> ma));
                      }
                      b.filter(filters);
                      return b;
                    }));

    // SearchRequest has no copy constructor, so relevant fields are copied manually.
    SearchRequest.Builder builder = new SearchRequest.Builder();
    builder.index(searchRequest.index());
    builder.from(searchRequest.from());
    builder.size(searchRequest.size());
    builder.query(mergedQuery);
    if (searchRequest.source() != null) {
      builder.source(searchRequest.source());
    }
    if (searchRequest.sort() != null) {
      builder.sort(searchRequest.sort());
    }
    if (searchRequest.highlight() != null) {
      builder.highlight(searchRequest.highlight());
    }
    if (searchRequest.aggregations() != null) {
      searchRequest.aggregations().forEach(builder::aggregations);
    }
    if (searchRequest.postFilter() != null) {
      builder.postFilter(searchRequest.postFilter());
    }
    if (searchRequest.trackTotalHits() != null) {
      builder.trackTotalHits(searchRequest.trackTotalHits());
    }
    return builder.build();
  }

  private void handleInterruptedException(Throwable ex) {
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
