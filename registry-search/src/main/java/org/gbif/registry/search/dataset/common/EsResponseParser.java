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
package org.gbif.registry.search.dataset.common;


import lombok.SneakyThrows;
import org.gbif.registry.search.dataset.indexing.ws.JacksonObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.gbif.api.model.common.search.Facet;
import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.api.model.common.search.SearchRequest;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch.core.search.CompletionSuggestOption;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.DoubleTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.FilterAggregate;

import static org.gbif.registry.search.dataset.indexing.es.EsQueryUtils.STRING_TO_DATE;
import static org.gbif.registry.search.dataset.indexing.es.EsQueryUtils.extractFacetLimit;
import static org.gbif.registry.search.dataset.indexing.es.EsQueryUtils.extractFacetOffset;

public class EsResponseParser<T, S, P extends SearchParameter> {

  private static final Pattern NESTED_PATTERN = Pattern.compile("^\\w+(\\.\\w+)+$");
  private static final Predicate<String> IS_NESTED = s -> NESTED_PATTERN.matcher(s).find();

  private static final Logger LOG = LoggerFactory.getLogger(EsResponseParser.class);

  private final EsFieldMapper<P> fieldParameterMapper;

  private final SearchResultConverter<T, S> searchResultConverter;

  /** Private constructor. */
  public EsResponseParser(
      SearchResultConverter<T, S> searchResultConverter, EsFieldMapper<P> fieldParameterMapper) {
    this.searchResultConverter = searchResultConverter;
    this.fieldParameterMapper = fieldParameterMapper;
  }

  /**
   * Builds a SearchResponse instance using the current builder state.
   *
   * @return a new instance of a SearchResponse.
   */
  @SneakyThrows
  public org.gbif.api.model.common.search.SearchResponse<T, P> buildSearchResponse(
      co.elastic.clients.elasticsearch.core.SearchResponse<com.fasterxml.jackson.databind.node.ObjectNode> esResponse, SearchRequest<P> request) {
    return buildSearchResponse(esResponse, request, hit -> {
      try {
        return searchResultConverter.toSearchResult(hit);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Builds a SearchResponse instance using the current builder state.
   *
   * @return a new instance of a SearchResponse.
   */
  @SneakyThrows
  public org.gbif.api.model.common.search.SearchResponse<S, P> buildSearchAutocompleteResponse(
      co.elastic.clients.elasticsearch.core.SearchResponse<com.fasterxml.jackson.databind.node.ObjectNode> esResponse, SearchRequest<P> request) {
    return buildSearchResponse(esResponse, request, hit -> {
      try {
        return searchResultConverter.toSearchSuggestResult(hit);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Builds a SearchResponse instance using the current builder state.
   *
   * @return a new instance of a SearchResponse.
   */
  public <R> org.gbif.api.model.common.search.SearchResponse<R, P> buildSearchResponse(
      co.elastic.clients.elasticsearch.core.SearchResponse<com.fasterxml.jackson.databind.node.ObjectNode> esResponse,
      SearchRequest<P> request,
      Function<Hit<com.fasterxml.jackson.databind.node.ObjectNode>, R> mapper) {

    org.gbif.api.model.common.search.SearchResponse<R, P> response = new org.gbif.api.model.common.search.SearchResponse<>(request);
    response.setCount(esResponse.hits().total().value());
    parseHits(esResponse, mapper).ifPresent(response::setResults);
    if (request instanceof FacetedSearchRequest) {
      parseFacets(esResponse, (FacetedSearchRequest<P>) request).ifPresent(response::setFacets);
    }

    return response;
  }

  public List<S> buildSuggestResponse(
      co.elastic.clients.elasticsearch.core.SearchResponse<com.fasterxml.jackson.databind.node.ObjectNode> esResponse, P parameter) {

    String fieldName = fieldParameterMapper.get(parameter);

    List<Suggestion<com.fasterxml.jackson.databind.node.ObjectNode>> suggesters = esResponse.suggest().get(fieldName);

    if (suggesters == null || suggesters.isEmpty()) {
      return Collections.emptyList();
    }

    return suggesters.stream()
      .filter(Suggestion::isCompletion)
      .flatMap(s -> s.completion().options().stream())
      .map(CompletionSuggestOption::source)
      .filter(Objects::nonNull)
      .map(source -> {
        Hit<com.fasterxml.jackson.databind.node.ObjectNode> hit = Hit.of(h -> h.source(source));
        try {
          return searchResultConverter.toSearchSuggestResult(hit);
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
      })
      .collect(Collectors.toList());
  }

  /** Simple data structure to hold bucket key and count. */
  private static class BucketData {
    final String key;
    final long docCount;

    BucketData(String key, long docCount) {
      this.key = key;
      this.docCount = docCount;
    }
  }

  /** Extract the buckets of an {@link Aggregation}. */
  private List<BucketData> getBuckets(co.elastic.clients.elasticsearch._types.aggregations.Aggregate aggregate) {
    if (aggregate.isSterms()) {
      StringTermsAggregate terms = aggregate.sterms();
      return terms.buckets().array().stream()
          .map(b -> new BucketData(b.key().stringValue(), b.docCount()))
          .collect(Collectors.toList());
    } else if (aggregate.isLterms()) {
      LongTermsAggregate terms = aggregate.lterms();
      return terms.buckets().array().stream()
          .map(b -> new BucketData(String.valueOf(b.key()), b.docCount()))
          .collect(Collectors.toList());
    } else if (aggregate.isDterms()) {
      DoubleTermsAggregate terms = aggregate.dterms();
      return terms.buckets().array().stream()
          .map(b -> new BucketData(String.valueOf(b.key()), b.docCount()))
          .collect(Collectors.toList());
    } else if (aggregate.isFilter()) {
      FilterAggregate filter = aggregate.filter();
      return filter.aggregations().entrySet().stream()
              .map(Entry::getValue)
              .flatMap(agg -> {
                if (agg.isSterms()) {
                  return agg.sterms().buckets().array().stream()
                      .map(b -> new BucketData(b.key().stringValue(), b.docCount()));
                } else if (agg.isLterms()) {
                  return agg.lterms().buckets().array().stream()
                      .map(b -> new BucketData(String.valueOf(b.key()), b.docCount()));
                } else if (agg.isDterms()) {
                  return agg.dterms().buckets().array().stream()
                      .map(b -> new BucketData(String.valueOf(b.key()), b.docCount()));
                } else {
                  return java.util.stream.Stream.<BucketData>empty();
                }
              })
              .collect(Collectors.toList());
    } else {
      throw new IllegalArgumentException(aggregate.getClass() + " aggregation not supported");
    }
  }

  private Optional<List<Facet<P>>> parseFacets(
      co.elastic.clients.elasticsearch.core.SearchResponse<com.fasterxml.jackson.databind.node.ObjectNode> esResponse, FacetedSearchRequest<P> request) {
    return Optional.ofNullable(esResponse.aggregations())
        .map(
            aggregations ->
                aggregations.entrySet().stream()
                    .map(
                        entry -> {
                          String aggName = entry.getKey();
                          Aggregate agg = entry.getValue();

                          // get buckets
                          List<BucketData> buckets = getBuckets(agg);

                          // get facet of the agg
                          P facet = fieldParameterMapper.get(aggName);

                          // check for paging in facets
                          long facetOffset = extractFacetOffset(request, facet);
                          long facetLimit = extractFacetLimit(request, facet);

                          List<Facet.Count> counts =
                              buckets.stream()
                                  .skip(facetOffset)
                                  .limit(facetOffset + facetLimit)
                                  .map(b -> new Facet.Count(b.key, b.docCount))
                                  .collect(Collectors.toList());

                          return new Facet<>(facet, counts);
                        })
                    .collect(Collectors.toList()));
  }

  private <R> Optional<List<R>> parseHits(
      co.elastic.clients.elasticsearch.core.SearchResponse<com.fasterxml.jackson.databind.node.ObjectNode> esResponse, Function<Hit<com.fasterxml.jackson.databind.node.ObjectNode>, R> mapper) {
    if (esResponse.hits() == null
        || esResponse.hits().hits() == null
        || esResponse.hits().hits().isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(
        esResponse.hits().hits().stream().map(mapper).collect(Collectors.toList()));
  }

  private static Optional<String> getStringValue(Hit<com.fasterxml.jackson.databind.node.ObjectNode> hit, String esField) {
    return getValue(hit, esField, Function.identity());
  }

  private static Optional<Integer> getIntValue(Hit<com.fasterxml.jackson.databind.node.ObjectNode> hit, String esField) {
    return getValue(hit, esField, Integer::valueOf);
  }

  private static Optional<Double> getDoubleValue(Hit<com.fasterxml.jackson.databind.node.ObjectNode> hit, String esField) {
    return getValue(hit, esField, Double::valueOf);
  }

  private static Optional<Date> getDateValue(Hit<com.fasterxml.jackson.databind.node.ObjectNode> hit, String esField) {
    return getValue(hit, esField, STRING_TO_DATE);
  }

  private static Optional<List<String>> getListValue(Hit<com.fasterxml.jackson.databind.node.ObjectNode> hit, String esField) {
    return Optional.ofNullable(getSourceAsMap(hit).get(esField))
        .map(v -> (List<String>) v)
        .filter(v -> !v.isEmpty());
  }

  private static Optional<List<Map<String, Object>>> getObjectsListValue(
      Hit<com.fasterxml.jackson.databind.node.ObjectNode> hit, String esField) {
    return Optional.ofNullable(getSourceAsMap(hit).get(esField))
        .map(v -> (List<Map<String, Object>>) v)
        .filter(v -> !v.isEmpty());
  }

  private static <T> Optional<T> getValue(
      Hit<com.fasterxml.jackson.databind.node.ObjectNode> hit, String esField, Function<String, T> mapper) {
    String fieldName = esField;
    Map<String, Object> fields = getSourceAsMap(hit);
    if (IS_NESTED.test(esField)) {
      // take all paths till the field name
      String[] paths = esField.split("\\.");
      for (int i = 0; i < paths.length - 1 && fields.containsKey(paths[i]); i++) {
        // update the fields with the current path
        fields = (Map<String, Object>) fields.get(paths[i]);
      }
      // the last path is the field name
      fieldName = paths[paths.length - 1];
    }

    return extractValue(fields, fieldName, mapper);
  }

  private static Map<String, Object> getSourceAsMap(Hit<com.fasterxml.jackson.databind.node.ObjectNode> hit) {
    try {
      com.fasterxml.jackson.databind.node.ObjectNode source = hit.source();
      if (source == null) {
        return new java.util.HashMap<>();
      }

      ObjectMapper mapper = JacksonObjectMapper.get();
      TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
      return mapper.convertValue(source, typeRef);
    } catch (Exception e) {
      LOG.error("Error parsing hit source as JSON: {}", e.getMessage(), e);
      return new java.util.HashMap<>();
    }
  }

  private static <T> Optional<T> extractValue(
      Map<String, Object> fields, String fieldName, Function<String, T> mapper) {
    return Optional.ofNullable(fields.get(fieldName))
        .map(String::valueOf)
        .filter(v -> !v.isEmpty())
        .map(
            v -> {
              try {
                return mapper.apply(v);
              } catch (Exception ex) {
                LOG.error("Error extracting field {} with value {}", fieldName, v);
                return null;
              }
            });
  }

  private static Optional<String> extractStringValue(Map<String, Object> fields, String fieldName) {
    return extractValue(fields, fieldName, Function.identity());
  }
}
