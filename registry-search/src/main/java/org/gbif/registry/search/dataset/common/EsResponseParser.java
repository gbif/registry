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
package org.gbif.registry.search.dataset.common;

import org.gbif.api.model.common.search.Facet;
import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.api.model.common.search.SearchRequest;
import org.gbif.api.model.common.search.SearchResponse;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  public SearchResponse<T, P> buildSearchResponse(
      org.elasticsearch.action.search.SearchResponse esResponse, SearchRequest<P> request) {
    return buildSearchResponse(esResponse, request, searchResultConverter::toSearchResult);
  }

  /**
   * Builds a SearchResponse instance using the current builder state.
   *
   * @return a new instance of a SearchResponse.
   */
  public SearchResponse<S, P> buildSearchAutocompleteResponse(
      org.elasticsearch.action.search.SearchResponse esResponse, SearchRequest<P> request) {
    return buildSearchResponse(esResponse, request, searchResultConverter::toSearchSuggestResult);
  }

  /**
   * Builds a SearchResponse instance using the current builder state.
   *
   * @return a new instance of a SearchResponse.
   */
  public <R> SearchResponse<R, P> buildSearchResponse(
      org.elasticsearch.action.search.SearchResponse esResponse,
      SearchRequest<P> request,
      Function<SearchHit, R> mapper) {

    SearchResponse<R, P> response = new SearchResponse<>(request);
    response.setCount(esResponse.getHits().getTotalHits());
    parseHits(esResponse, mapper).ifPresent(response::setResults);
    if (request instanceof FacetedSearchRequest) {
      parseFacets(esResponse, (FacetedSearchRequest<P>) request).ifPresent(response::setFacets);
    }

    return response;
  }

  public List<S> buildSuggestResponse(
      org.elasticsearch.action.search.SearchResponse esResponse, P parameter) {

    String fieldName = fieldParameterMapper.get(parameter);

    return esResponse.getSuggest().getSuggestion(fieldName).getEntries().stream()
        .flatMap(e -> ((CompletionSuggestion.Entry) e).getOptions().stream())
        .map(CompletionSuggestion.Entry.Option::getHit)
        .filter(Objects::nonNull)
        .map(searchResultConverter::toSearchSuggestResult)
        .collect(Collectors.toList());
  }

  /** Extract the buckets of an {@link Aggregation}. */
  private List<? extends Terms.Bucket> getBuckets(Aggregation aggregation) {
    if (aggregation instanceof Terms) {
      return ((Terms) aggregation).getBuckets();
    } else if (aggregation instanceof Filter) {
      return ((Filter) aggregation)
          .getAggregations().asList().stream()
              .flatMap(agg -> ((Terms) agg).getBuckets().stream())
              .collect(Collectors.toList());
    } else {
      throw new IllegalArgumentException(aggregation.getClass() + " aggregation not supported");
    }
  }

  private Optional<List<Facet<P>>> parseFacets(
      org.elasticsearch.action.search.SearchResponse esResponse, FacetedSearchRequest<P> request) {
    return Optional.ofNullable(esResponse.getAggregations())
        .map(
            aggregations ->
                aggregations.asList().stream()
                    .map(
                        aggs -> {
                          // get buckets
                          List<? extends Terms.Bucket> buckets = getBuckets(aggs);

                          // get facet of the agg
                          P facet = fieldParameterMapper.get(aggs.getName());

                          // check for paging in facets
                          long facetOffset = extractFacetOffset(request, facet);
                          long facetLimit = extractFacetLimit(request, facet);

                          List<Facet.Count> counts =
                              buckets.stream()
                                  .skip(facetOffset)
                                  .limit(facetOffset + facetLimit)
                                  .map(b -> new Facet.Count(b.getKeyAsString(), b.getDocCount()))
                                  .collect(Collectors.toList());

                          return new Facet<>(facet, counts);
                        })
                    .collect(Collectors.toList()));
  }

  private <R> Optional<List<R>> parseHits(
      org.elasticsearch.action.search.SearchResponse esResponse, Function<SearchHit, R> mapper) {
    if (esResponse.getHits() == null
        || esResponse.getHits().getHits() == null
        || esResponse.getHits().getHits().length == 0) {
      return Optional.empty();
    }

    return Optional.of(
        Stream.of(esResponse.getHits().getHits()).map(mapper).collect(Collectors.toList()));
  }

  private static Optional<String> getStringValue(SearchHit hit, String esField) {
    return getValue(hit, esField, Function.identity());
  }

  private static Optional<Integer> getIntValue(SearchHit hit, String esField) {
    return getValue(hit, esField, Integer::valueOf);
  }

  private static Optional<Double> getDoubleValue(SearchHit hit, String esField) {
    return getValue(hit, esField, Double::valueOf);
  }

  private static Optional<Date> getDateValue(SearchHit hit, String esField) {
    return getValue(hit, esField, STRING_TO_DATE);
  }

  private static Optional<List<String>> getListValue(SearchHit hit, String esField) {
    return Optional.ofNullable(hit.getSourceAsMap().get(esField))
        .map(v -> (List<String>) v)
        .filter(v -> !v.isEmpty());
  }

  private static Optional<List<Map<String, Object>>> getObjectsListValue(
      SearchHit hit, String esField) {
    return Optional.ofNullable(hit.getSourceAsMap().get(esField))
        .map(v -> (List<Map<String, Object>>) v)
        .filter(v -> !v.isEmpty());
  }

  private static <T> Optional<T> getValue(
      SearchHit hit, String esField, Function<String, T> mapper) {
    String fieldName = esField;
    Map<String, Object> fields = hit.getSourceAsMap();
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
