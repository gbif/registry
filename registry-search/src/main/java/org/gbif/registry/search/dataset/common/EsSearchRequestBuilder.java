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

import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchConstants;
import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.geo.ShapeRelation;
import org.elasticsearch.common.geo.builders.CoordinatesBuilder;
import org.elasticsearch.common.geo.builders.LineStringBuilder;
import org.elasticsearch.common.geo.builders.MultiPolygonBuilder;
import org.elasticsearch.common.geo.builders.PointBuilder;
import org.elasticsearch.common.geo.builders.PolygonBuilder;
import org.elasticsearch.common.geo.builders.ShapeBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.GeoShapeQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import com.google.common.annotations.VisibleForTesting;

import static org.gbif.api.util.SearchTypeValidator.isRange;
import static org.gbif.registry.search.dataset.indexing.es.EsQueryUtils.LOWER_BOUND_RANGE_PARSER;
import static org.gbif.registry.search.dataset.indexing.es.EsQueryUtils.RANGE_SEPARATOR;
import static org.gbif.registry.search.dataset.indexing.es.EsQueryUtils.RANGE_WILDCARD;
import static org.gbif.registry.search.dataset.indexing.es.EsQueryUtils.UPPER_BOUND_RANGE_PARSER;
import static org.gbif.registry.search.dataset.indexing.es.EsQueryUtils.extractFacetLimit;
import static org.gbif.registry.search.dataset.indexing.es.EsQueryUtils.extractFacetOffset;

public class EsSearchRequestBuilder<P extends SearchParameter> {

  private static final int MAX_SIZE_TERMS_AGGS = 1200000;
  private static final IntUnaryOperator DEFAULT_SHARD_SIZE = size -> (size * 2) + 50000;
  private static final String PRE_HL_TAG = "<em class=\"gbifHl\">";
  private static final String POST_HL_TAG = "</em>";

  private final EsFieldMapper<P> esFieldMapper;

  // this instance is created only once and reused for all searches
  private final HighlightBuilder highlightBuilder =
      new HighlightBuilder()
          .forceSource(true)
          .preTags(PRE_HL_TAG)
          .postTags(POST_HL_TAG)
          .encoder("html")
          .highlighterType("unified")
          .requireFieldMatch(false)
          .numOfFragments(0);

  public EsSearchRequestBuilder(EsFieldMapper<P> esFieldMapper) {
    this.esFieldMapper = esFieldMapper;
    Arrays.stream(esFieldMapper.highlightingFields()).forEach(highlightBuilder::field);
  }

  public SearchRequest buildSearchRequest(
      FacetedSearchRequest<P> searchRequest, boolean facetsEnabled, String index) {

    SearchRequest esRequest = new SearchRequest();
    esRequest.indices(index);

    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    esRequest.source(searchSourceBuilder);
    searchSourceBuilder.fetchSource(null, esFieldMapper.excludeFields());

    // size and offset
    searchSourceBuilder.size(searchRequest.getLimit());
    searchSourceBuilder.from((int) searchRequest.getOffset());

    // sort
    if (Strings.isNullOrEmpty(searchRequest.getQ())) {
      for (SortBuilder sb : esFieldMapper.sorts()) {
        searchSourceBuilder.sort(sb);
      }
    } else {
      searchSourceBuilder.sort(SortBuilders.scoreSort());
      if (searchRequest.isHighlight()) {
        searchSourceBuilder.highlighter(highlightBuilder);
      }
    }

    // group params
    GroupedParams<P> groupedParams = groupParameters(searchRequest);

    // add query
    if (SearchConstants.QUERY_WILDCARD.equals(searchRequest.getQ())) { // Is a search all
      searchSourceBuilder.query(QueryBuilders.matchAllQuery());
    } else {
      buildQuery(groupedParams.queryParams, searchRequest.getQ())
          .ifPresent(searchSourceBuilder::query);
    }

    // add aggs
    buildAggs(searchRequest, groupedParams.postFilterParams, facetsEnabled)
        .ifPresent(aggsList -> aggsList.forEach(searchSourceBuilder::aggregation));

    // post-filter
    buildPostFilter(groupedParams.postFilterParams).ifPresent(searchSourceBuilder::postFilter);

    return esRequest;
  }

  public Optional<QueryBuilder> buildQueryNode(FacetedSearchRequest<P> searchRequest) {
    return buildQuery(searchRequest.getParameters(), searchRequest.getQ());
  }

  public SearchRequest buildSuggestQuery(String prefix, P parameter, Integer limit, String index) {
    SearchRequest request = new SearchRequest();
    request.indices(index);

    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    request.source(searchSourceBuilder);

    String esField = esFieldMapper.get(parameter);

    // create suggest query
    searchSourceBuilder.suggest(
        new SuggestBuilder()
            .addSuggestion(
                esField,
                SuggestBuilders.completionSuggestion(esField + ".suggest")
                    .prefix(prefix)
                    .size(limit != null ? limit : SearchConstants.DEFAULT_SUGGEST_LIMIT)
                    .skipDuplicates(true)));

    // add source field
    searchSourceBuilder.fetchSource(
        esFieldMapper.includeSuggestFields(parameter), esFieldMapper.excludeFields());

    return request;
  }

  private Optional<QueryBuilder> buildQuery(Map<P, Set<String>> params, String qParam) {
    // create bool node
    BoolQueryBuilder bool = QueryBuilders.boolQuery();

    // adding full text search parameter
    if (!Strings.isNullOrEmpty(qParam)) {
      bool.must(esFieldMapper.fullTextQuery(qParam));
    }

    if (params != null && !params.isEmpty()) {
      // adding geometry to bool
      if (params.containsKey(OccurrenceSearchParameter.GEOMETRY)) {
        BoolQueryBuilder shouldGeometry = QueryBuilders.boolQuery();
        shouldGeometry
            .should()
            .addAll(
                params.get((P) OccurrenceSearchParameter.GEOMETRY).stream()
                    .map(EsSearchRequestBuilder::buildGeoShapeQuery)
                    .collect(Collectors.toList()));
        bool.filter().add(shouldGeometry);
      }

      // adding term queries to bool
      bool.filter()
          .addAll(
              params.entrySet().stream()
                  .filter(e -> Objects.nonNull(esFieldMapper.get(e.getKey())))
                  .flatMap(
                      e ->
                          buildTermQuery(e.getValue(), e.getKey(), esFieldMapper.get(e.getKey()))
                              .stream())
                  .collect(Collectors.toList()));
    }

    return bool.must().isEmpty() && bool.filter().isEmpty() ? Optional.empty() : Optional.of(bool);
  }

  @VisibleForTesting
  GroupedParams groupParameters(FacetedSearchRequest<P> searchRequest) {
    GroupedParams groupedParams = new GroupedParams<P>();

    if (!searchRequest.isMultiSelectFacets()
        || searchRequest.getFacets() == null
        || searchRequest.getFacets().isEmpty()) {
      groupedParams.queryParams = searchRequest.getParameters();
      return groupedParams;
    }

    groupedParams.queryParams = new HashMap<>();
    groupedParams.postFilterParams = new HashMap<>();

    searchRequest
        .getParameters()
        .forEach(
            (k, v) -> {
              if (searchRequest.getFacets().contains(k)) {
                groupedParams.postFilterParams.put(k, v);
              } else {
                groupedParams.queryParams.put(k, v);
              }
            });

    return groupedParams;
  }

  private Optional<QueryBuilder> buildPostFilter(Map<P, Set<String>> postFilterParams) {
    if (postFilterParams == null || postFilterParams.isEmpty()) {
      return Optional.empty();
    }

    BoolQueryBuilder bool = QueryBuilders.boolQuery();
    bool.filter()
        .addAll(
            postFilterParams.entrySet().stream()
                .flatMap(
                    e ->
                        buildTermQuery(e.getValue(), e.getKey(), esFieldMapper.get(e.getKey()))
                            .stream())
                .collect(Collectors.toList()));

    return Optional.of(bool);
  }

  private Optional<List<AggregationBuilder>> buildAggs(
      FacetedSearchRequest<P> searchRequest,
      Map<P, Set<String>> postFilterParams,
      boolean facetsEnabled) {
    if (!facetsEnabled
        || searchRequest.getFacets() == null
        || searchRequest.getFacets().isEmpty()) {
      return Optional.empty();
    }

    if (searchRequest.isMultiSelectFacets()
        && postFilterParams != null
        && !postFilterParams.isEmpty()) {
      return Optional.of(buildFacetsMultiselect(searchRequest, postFilterParams));
    }

    return Optional.of(buildFacets(searchRequest));
  }

  private List<AggregationBuilder> buildFacetsMultiselect(
      FacetedSearchRequest<P> searchRequest, Map<P, Set<String>> postFilterParams) {

    if (searchRequest.getFacets().size() == 1) {
      // same case as normal facets
      return buildFacets(searchRequest);
    }

    return searchRequest.getFacets().stream()
        .filter(p -> esFieldMapper.get(p) != null)
        .map(
            facetParam -> {

              // build filter aggs
              BoolQueryBuilder bool = QueryBuilders.boolQuery();
              bool.filter()
                  .addAll(
                      postFilterParams.entrySet().stream()
                          .filter(entry -> entry.getKey() != facetParam)
                          .flatMap(
                              e ->
                                  buildTermQuery(
                                      e.getValue(), e.getKey(), esFieldMapper.get(e.getKey()))
                                      .stream())
                          .collect(Collectors.toList()));

              // add filter to the aggs
              String esField = esFieldMapper.get(facetParam);
              FilterAggregationBuilder filterAggs = AggregationBuilders.filter(esField, bool);

              // build terms aggs and add it to the filter aggs
              TermsAggregationBuilder termsAggs =
                  buildTermsAggs(
                      "filtered_" + esField,
                      esField,
                      extractFacetOffset(searchRequest, facetParam),
                      extractFacetLimit(searchRequest, facetParam),
                      searchRequest.getFacetMinCount());
              filterAggs.subAggregation(termsAggs);

              return filterAggs;
            })
        .collect(Collectors.toList());
  }

  private List<AggregationBuilder> buildFacets(FacetedSearchRequest<P> searchRequest) {
    return searchRequest.getFacets().stream()
        .filter(p -> esFieldMapper.get(p) != null)
        .map(
            facetParam -> {
              String esField = esFieldMapper.get(facetParam);
              return buildTermsAggs(
                  esField,
                  esField,
                  extractFacetOffset(searchRequest, facetParam),
                  extractFacetLimit(searchRequest, facetParam),
                  searchRequest.getFacetMinCount());
            })
        .collect(Collectors.toList());
  }

  private TermsAggregationBuilder buildTermsAggs(
      String aggsName, String esField, int facetOffset, int facetLimit, Integer minCount) {
    // build aggs for the field
    TermsAggregationBuilder termsAggsBuilder = AggregationBuilders.terms(aggsName).field(esField);

    // min count
    Optional.ofNullable(minCount).ifPresent(termsAggsBuilder::minDocCount);

    // aggs size
    int size = calculateAggsSize(esField, facetOffset, facetLimit);
    termsAggsBuilder.size(size);

    // aggs shard size
    termsAggsBuilder.shardSize(
        Optional.ofNullable(esFieldMapper.getCardinality(esField))
            .orElse(DEFAULT_SHARD_SIZE.applyAsInt(size)));

    return termsAggsBuilder;
  }

  private int calculateAggsSize(String esField, int facetOffset, int facetLimit) {
    int maxCardinality =
        Optional.ofNullable(esFieldMapper.getCardinality(esField)).orElse(Integer.MAX_VALUE);

    // the limit is bounded by the max cardinality of the field
    int limit = Math.min(facetOffset + facetLimit, maxCardinality);

    // we set a maximum limit for performance reasons
    if (limit > MAX_SIZE_TERMS_AGGS) {
      throw new IllegalArgumentException(
          "Facets paging is only supported up to " + MAX_SIZE_TERMS_AGGS + " elements");
    }
    return limit;
  }

  /**
   * Mapping parameter values into know values for Enums. Non-enum parameter values are passed using
   * its raw value. Country can be enum value or iso code.
   */
  private String parseParamValue(String value, P parameter) {
    if (Enum.class.isAssignableFrom(parameter.type())) {
      if (!Country.class.isAssignableFrom(parameter.type())) {
        return VocabularyUtils.lookup(value, (Class<Enum<?>>) parameter.type())
            .map(Enum::name)
            .orElse(null);
      } else {
        return VocabularyUtils.lookup(value, Country.class)
            .map(Country::getIso2LetterCode)
            .orElse(value);
      }
    }

    if (Boolean.class.isAssignableFrom(parameter.type())) {
      return value.toLowerCase();
    }
    return value;
  }

  private List<QueryBuilder> buildTermQuery(Collection<String> values, P param, String esField) {
    List<QueryBuilder> queries = new ArrayList<>();

    // collect queries for each value
    List<String> parsedValues = new ArrayList<>();
    for (String value : values) {
      if (isRange(value)) {
        queries.add(buildRangeQuery(esField, value));
        continue;
      }

      parsedValues.add(parseParamValue(value, param));
    }

    if (parsedValues.size() == 1) {
      // single term
      queries.add(QueryBuilders.termQuery(esField, parsedValues.get(0)));
    } else if (parsedValues.size() > 1) {
      // multi term query
      queries.add(QueryBuilders.termsQuery(esField, parsedValues));
    }

    return queries;
  }

  private RangeQueryBuilder buildRangeQuery(String esField, String value) {
    RangeQueryBuilder builder = QueryBuilders.rangeQuery(esField);

    if (esFieldMapper.isDateField(esField)) {
      String[] values = value.split(RANGE_SEPARATOR);

      LocalDateTime lowerBound = LOWER_BOUND_RANGE_PARSER.apply(values[0]);
      if (lowerBound != null) {
        builder.gte(lowerBound);
      }

      LocalDateTime upperBound = UPPER_BOUND_RANGE_PARSER.apply(values[1]);
      if (upperBound != null) {
        builder.lte(upperBound);
      }
    } else {
      String[] values = value.split(RANGE_SEPARATOR);
      if (!RANGE_WILDCARD.equals(values[0])) {
        builder.gte(values[0]);
      }
      if (!RANGE_WILDCARD.equals(values[1])) {
        builder.lte(values[1]);
      }
    }

    return builder;
  }

  public static GeoShapeQueryBuilder buildGeoShapeQuery(String wkt) {
    Geometry geometry;
    try {
      geometry = new WKTReader().read(wkt);
    } catch (ParseException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }

    Function<Polygon, PolygonBuilder> polygonToBuilder =
        polygon -> {
          PolygonBuilder polygonBuilder =
              new PolygonBuilder(
                  new CoordinatesBuilder()
                      .coordinates(
                          normalizePolygonCoordinates(polygon.getExteriorRing().getCoordinates())));
          for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            polygonBuilder.hole(
                new LineStringBuilder(
                    new CoordinatesBuilder()
                        .coordinates(
                            normalizePolygonCoordinates(
                                polygon.getInteriorRingN(i).getCoordinates()))));
          }
          return polygonBuilder;
        };

    String type =
        "LinearRing".equals(geometry.getGeometryType())
            ? "LINESTRING"
            : geometry.getGeometryType().toUpperCase();

    ShapeBuilder shapeBuilder = null;
    if (("POINT").equals(type)) {
      shapeBuilder = new PointBuilder(geometry.getCoordinate().x, geometry.getCoordinate().y);
    } else if ("LINESTRING".equals(type)) {
      shapeBuilder = new LineStringBuilder(Arrays.asList(geometry.getCoordinates()));
    } else if ("POLYGON".equals(type)) {
      shapeBuilder = polygonToBuilder.apply((Polygon) geometry);
    } else if ("MULTIPOLYGON".equals(type)) {
      // multipolygon
      MultiPolygonBuilder multiPolygonBuilder = new MultiPolygonBuilder();
      for (int i = 0; i < geometry.getNumGeometries(); i++) {
        multiPolygonBuilder.polygon(polygonToBuilder.apply((Polygon) geometry.getGeometryN(i)));
      }
      shapeBuilder = multiPolygonBuilder;
    } else {
      throw new IllegalArgumentException(type + " shape is not supported");
    }

    try {
      return QueryBuilders.geoShapeQuery("coordinate", shapeBuilder).relation(ShapeRelation.WITHIN);
    } catch (IOException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  /** Eliminates consecutive duplicates. The order is preserved. */
  @VisibleForTesting
  static Coordinate[] normalizePolygonCoordinates(Coordinate[] coordinates) {
    List<Coordinate> normalizedCoordinates = new ArrayList<>();

    // we always have to keep the fist and last coordinates
    int i = 0;
    normalizedCoordinates.add(i++, coordinates[0]);

    for (int j = 1; j < coordinates.length; j++) {
      if (!coordinates[j - 1].equals(coordinates[j])) {
        normalizedCoordinates.add(i++, coordinates[j]);
      }
    }

    return normalizedCoordinates.toArray(new Coordinate[0]);
  }

  @VisibleForTesting
  static class GroupedParams<P extends SearchParameter> {
    Map<P, Set<String>> postFilterParams;
    Map<P, Set<String>> queryParams;
  }
}
