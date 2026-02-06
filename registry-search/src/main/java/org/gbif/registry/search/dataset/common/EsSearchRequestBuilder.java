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

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch.core.SearchRequest;

import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.HighlighterEncoder;
import co.elastic.clients.json.JsonData;

import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchConstants;
import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;

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
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;

import org.gbif.registry.search.dataset.indexing.es.LocalEmbeddingService;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import static org.gbif.api.util.SearchTypeValidator.isNumericRange;
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
  private final LocalEmbeddingService embeddingService;

  public EsSearchRequestBuilder(EsFieldMapper<P> esFieldMapper, LocalEmbeddingService embeddingService) {
    this.esFieldMapper = esFieldMapper;
    this.embeddingService = embeddingService;
  }

  private Highlight buildHighlight() {
    Map<String, HighlightField> fields = new HashMap<>();
    Arrays.stream(esFieldMapper.highlightingFields()).forEach(field -> fields.put(field, HighlightField.of(hf -> hf
      .numberOfFragments(0)
    )));

    return Highlight.of(h -> h
      .preTags(PRE_HL_TAG)
      .postTags(POST_HL_TAG)
      .encoder(HighlighterEncoder.Html)
      .type("unified")
      .requireFieldMatch(false)
      .fields(fields)
    );
  }

  public SearchRequest buildSearchRequest(
    FacetedSearchRequest<P> searchRequest, boolean facetsEnabled, String index, boolean semanticSearch) {

    SearchRequest.Builder builder = new SearchRequest.Builder();
    builder.index(index);
    builder.trackTotalHits(t -> t.enabled(true));

    // source filtering
    String[] includes = esFieldMapper.getMappedFields();
    String[] excludes = esFieldMapper.excludeFields();
    if (includes != null || excludes != null) {
      builder.source(s -> s.filter(f -> f.includes(Arrays.asList(includes != null ? includes : new String[0]))
        .excludes(Arrays.asList(excludes != null ? excludes : new String[0]))));
    }

    // size and offset
    builder.size(searchRequest.getLimit());
    builder.from((int) searchRequest.getOffset());

    // group params
    GroupedParams<P> groupedParams = groupParameters(searchRequest);

    // sort and highlighting
    if (Strings.isNullOrEmpty(searchRequest.getQ())) {
      SortOptions[] sorts = esFieldMapper.sorts();
      if (sorts != null && sorts.length > 0) {
        builder.sort(Arrays.asList(sorts));
      }
    } else {
      builder.sort(SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc))));
      if (searchRequest.isHighlight() && !semanticSearch) {
        builder.highlight(buildHighlight());
      }
    }

    // Semantic search with kNN
    if (semanticSearch && !Strings.isNullOrEmpty(searchRequest.getQ())
      && !SearchConstants.QUERY_WILDCARD.equals(searchRequest.getQ())) {
      float[] queryVector = embeddingService.generateEmbedding(searchRequest.getQ());
      List<Float> vectorList = toFloatList(queryVector);

      builder.knn(knn -> knn
        .field("embedding")
        .queryVector(vectorList)
        .k(searchRequest.getLimit())
        .numCandidates(Math.max(100, searchRequest.getLimit() * 2))
      );

      // Add filter for kNN if there are filter params
      buildQuery(groupedParams.queryParams, null).ifPresent(filterQuery ->
        builder.knn(knn -> knn
          .field("embedding")
          .queryVector(vectorList)
          .k(searchRequest.getLimit())
          .numCandidates(Math.max(100, searchRequest.getLimit() * 2))
          .filter(filterQuery)
        )
      );
    } else {
      // Traditional full-text search
      if (SearchConstants.QUERY_WILDCARD.equals(searchRequest.getQ())) {
        builder.query(Query.of(q -> q.matchAll(ma -> ma)));
      } else {
        buildQuery(groupedParams.queryParams, searchRequest.getQ())
          .ifPresent(builder::query);
      }
    }

    // add aggs
    buildAggs(searchRequest, groupedParams.postFilterParams, facetsEnabled)
      .ifPresent(aggsMap -> aggsMap.forEach(builder::aggregations));

    // post-filter
    buildPostFilter(groupedParams.postFilterParams).ifPresent(builder::postFilter);

    return builder.build();
  }

  public SearchRequest buildSearchRequest(
    FacetedSearchRequest<P> searchRequest, boolean facetsEnabled, String index) {
    return buildSearchRequest(searchRequest, facetsEnabled, index, false);
  }

  private List<Float> toFloatList(float[] arr) {
    List<Float> list = new ArrayList<>(arr.length);
    for (float f : arr) {
      list.add(f);
    }
    return list;
  }

  public SearchRequest buildAutocompleteQuery(
      org.gbif.api.model.common.search.SearchRequest<P> searchRequest, P parameter, String index) {
    Optional<Query> filterQuery = buildQuery(searchRequest.getParameters(), null);

    List<Query> shouldClauses = new ArrayList<>();
    List<Query> mustClauses = new ArrayList<>();

    if (!Strings.isNullOrEmpty(searchRequest.getQ())) {
      // Main autocomplete match query
      String autocompleteField = esFieldMapper.getAutocompleteField(parameter);
      if (autocompleteField != null) {
        shouldClauses.add(Query.of(q -> q.match(m -> m
          .field(autocompleteField)
          .query(searchRequest.getQ())
          .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
        )));
      }

      // Prefix boost for short queries
      if (searchRequest.getQ().length() > 2) {
        String field = esFieldMapper.get(parameter);
        if (field != null) {
          shouldClauses.add(Query.of(q -> q.prefix(p -> p
            .field(field)
            .value(searchRequest.getQ().toLowerCase())
            .boost(100.0f)
          )));
        }
      }
    } else {
      mustClauses.add(Query.of(q -> q.matchAll(ma -> ma)));
    }

    filterQuery.ifPresent(mustClauses::add);

    Query mainQuery = Query.of(q -> q.bool(b -> {
      if (!shouldClauses.isEmpty()) {
        b.should(shouldClauses);
        b.minimumShouldMatch("1");
      }
      if (!mustClauses.isEmpty()) {
        b.must(mustClauses);
      }
      return b;
    }));

    SearchRequest.Builder builder = new SearchRequest.Builder();
    builder.index(index);
    builder.size(searchRequest.getLimit());
    builder.from(Math.max(0, (int) searchRequest.getOffset()));
    builder.query(mainQuery);

    // source filtering
    String[] includes = esFieldMapper.includeSuggestFields(parameter);
    String[] excludes = esFieldMapper.excludeFields();
    if (includes != null || excludes != null) {
      builder.source(s -> s.filter(f -> f.includes(Arrays.asList(includes != null ? includes : new String[0]))
                                        .excludes(Arrays.asList(excludes != null ? excludes : new String[0]))));
    }

    return builder.build();
  }
/*
  private Optional<QueryBuilder> buildQuery(Map<P, Set<String>> params, String qParam) {
    // create bool node
    BoolQuery bool = co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.bool().build();

    // adding full text search parameter
    if (!Strings.isNullOrEmpty(qParam)) {
      bool.must().add(esFieldMapper.fullTextQuery(qParam));
    }

    if (params != null && !params.isEmpty()) {
      // adding geometry to bool
      if (params.containsKey(OccurrenceSearchParameter.GEOMETRY)) {
        BoolQuery shouldGeometry = co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.bool().build();
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
*/
  private Optional<Query> buildQuery(Map<P, Set<String>> params, String qParam) {
    List<Query> mustClauses = new ArrayList<>();
    List<Query> filterClauses = new ArrayList<>();

  // Full-text search
  if (!Strings.isNullOrEmpty(qParam)) {
    mustClauses.add(esFieldMapper.fullTextQuery(qParam));
  }

  if (params != null && !params.isEmpty()) {
    // Geometry
    if (params.containsKey(OccurrenceSearchParameter.GEOMETRY)) {
      List<Query> geoQueries = params.get((P) OccurrenceSearchParameter.GEOMETRY).stream()
        .map(EsSearchRequestBuilder::buildGeoShapeQuery)
        .collect(Collectors.toList());

      Query geoBoolQuery = Query.of(q -> q.bool(b -> b.should(geoQueries)));
      filterClauses.add(geoBoolQuery);
    }

    // Term filters
    filterClauses.addAll(
      (Collection<? extends Query>) params.entrySet().stream()
        .filter(e -> Objects.nonNull(esFieldMapper.get(e.getKey())))
        .flatMap(
          e -> buildTermQuery(e.getValue(), e.getKey(), esFieldMapper.get(e.getKey()))
            .stream())
        .collect(Collectors.toList())
    );
  }

  if (mustClauses.isEmpty() && filterClauses.isEmpty()) {
    return Optional.empty();
  }

  return Optional.of(Query.of(q -> q.bool(b -> b
    .must(mustClauses)
    .filter(filterClauses)
  )));
}

  @VisibleForTesting
  GroupedParams groupParameters(FacetedSearchRequest<P> searchRequest) {
    GroupedParams groupedParams = new GroupedParams<P>();

    if (!searchRequest.isFacetMultiSelect()
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

  private Optional<Query> buildPostFilter(Map<P, Set<String>> postFilterParams) {
    if (postFilterParams == null || postFilterParams.isEmpty()) {
      return Optional.empty();
    }

    List<Query> filterClauses = postFilterParams.entrySet().stream()
      .filter(e -> Objects.nonNull(esFieldMapper.get(e.getKey())))
      .flatMap(e -> buildTermQuery(e.getValue(), e.getKey(), esFieldMapper.get(e.getKey())).stream())
      .collect(Collectors.toList());

    if (filterClauses.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(Query.of(q -> q.bool(b -> b.filter(filterClauses))));
  }

  private Optional<Map<String, Aggregation>> buildAggs(
      FacetedSearchRequest<P> searchRequest,
      Map<P, Set<String>> postFilterParams,
      boolean facetsEnabled) {
    if (!facetsEnabled
        || searchRequest.getFacets() == null
        || searchRequest.getFacets().isEmpty()) {
      return Optional.empty();
    }

    if (searchRequest.isFacetMultiSelect()
        && postFilterParams != null
        && !postFilterParams.isEmpty()) {
      return Optional.of(buildFacetsMultiselect(searchRequest, postFilterParams));
    }

    return Optional.of(buildFacets(searchRequest));
  }

  private Map<String, Aggregation> buildFacetsMultiselect(
      FacetedSearchRequest<P> searchRequest, Map<P, Set<String>> postFilterParams) {

    if (searchRequest.getFacets().size() == 1) {
      // same case as normal facets
      return buildFacets(searchRequest);
    }

    Map<String, Aggregation> aggregations = new HashMap<>();

    for (P facetParam : searchRequest.getFacets()) {
      if (esFieldMapper.get(facetParam) == null) continue;

      String esField = esFieldMapper.get(facetParam);

      // build filter queries for other facet parameters
      List<Query> filterQueries = postFilterParams.entrySet().stream()
        .filter(entry -> entry.getKey() != facetParam)
        .filter(e -> Objects.nonNull(esFieldMapper.get(e.getKey())))
        .flatMap(e -> buildTermQuery(e.getValue(), e.getKey(), esFieldMapper.get(e.getKey())).stream())
        .collect(Collectors.toList());

      Query filterQuery = filterQueries.isEmpty()
        ? Query.of(q -> q.matchAll(ma -> ma))
        : Query.of(q -> q.bool(b -> b.filter(filterQueries)));

      // build terms aggregation
      TermsAggregation termsAgg = buildTermsAggs(
        "filtered_" + esField,
        esField,
        extractFacetOffset(searchRequest, facetParam),
        extractFacetLimit(searchRequest, facetParam),
        searchRequest.getFacetMinCount());

      // wrap in filter aggregation
      Aggregation filterAgg = Aggregation.of(a -> a
        .filter(filterQuery)
        .aggregations("filtered_" + esField, termsAgg)
      );

      aggregations.put(esField, filterAgg);
    }

    return aggregations;
  }

  private Map<String, Aggregation> buildFacets(FacetedSearchRequest<P> searchRequest) {
    Map<String, Aggregation> aggregations = new HashMap<>();

    for (P facetParam : searchRequest.getFacets()) {
      if (esFieldMapper.get(facetParam) == null) continue;

      String esField = esFieldMapper.get(facetParam);
      TermsAggregation aggregation = buildTermsAggs(
        esField,
        esField,
        extractFacetOffset(searchRequest, facetParam),
        extractFacetLimit(searchRequest, facetParam),
        searchRequest.getFacetMinCount());

      aggregations.put(esField, aggregation._toAggregation());
    }

    return aggregations;
  }

  private TermsAggregation buildTermsAggs(
    String aggsName, String esField, int facetOffset, int facetLimit, Integer minCount) {

    int size = calculateAggsSize(esField, facetOffset, facetLimit);

    return new TermsAggregation.Builder()
      .field(esField)
      .size(size)
      .minDocCount(minCount != null ? minCount : 1) // veya default
      .build();
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

  private List<Query> buildTermQuery(Collection<String> values, P param, String esField) {
    List<Query> queries = new ArrayList<>();

    // collect queries for each value
    List<String> parsedValues = new ArrayList<>();
    for (String value : values) {
      if (isNumericRange(value)) {
        queries.add(buildRangeQuery(esField, value));
        continue;
      }

      parsedValues.add(parseParamValue(value, param));
    }
    queries.add(buildTermOrTermsQuery(esField, parsedValues));

    return queries;
  }


  /**
   * Builds a term or terms query for regular fields.
   */
  private Query buildTermOrTermsQuery(String esField, List<String> values) {
    if (values.size() == 1) {
      return Query.of(q -> q.term(t -> t.field(esField).value(FieldValue.of(values.get(0)))));
    } else {
      List<FieldValue> fieldValues = values.stream()
        .map(FieldValue::of)
        .toList();
      return Query.of(q -> q.terms(t -> t.field(esField).terms(ts -> ts.value(fieldValues))));
    }
  }
  /**
   * Builds range queries for numeric and date fields.
   */
  private Query buildRangeQuery(String esField, String value) {
    String[] values = value.split(RANGE_SEPARATOR);

    if (values.length != 2) {
      throw new IllegalArgumentException("Invalid range format: " + value);
    }

    if (esFieldMapper.isDateField(esField)) {
      return buildDateRangeQuery(esField, values);
    } else {
      return buildNumericRangeQuery(esField, values);
    }
  }

  /**
   * Builds a date range query.
   */
  private Query buildDateRangeQuery(String esField, String[] values) {
    LocalDateTime lower = LOWER_BOUND_RANGE_PARSER.apply(values[0]);
    LocalDateTime upper = UPPER_BOUND_RANGE_PARSER.apply(values[1]);

    return Query.of(q -> q.range(r -> r.date(d -> {
      d.field(esField);
      if (lower != null) d.gte(lower.toString());
      if (upper != null) d.lte(upper.toString());
      return d;
    })));
  }

  /**
   * Builds a numeric range query.
   */
  private Query buildNumericRangeQuery(String esField, String[] values) {
    // Try to parse as numbers first
    try {
      Double lowerBound = RANGE_WILDCARD.equals(values[0]) ? null : Double.parseDouble(values[0]);
      Double upperBound = RANGE_WILDCARD.equals(values[1]) ? null : Double.parseDouble(values[1]);

      return Query.of(q -> q.range(r -> r.number(n -> {
        n.field(esField);
        if (lowerBound != null) n.gte(lowerBound);
        if (upperBound != null) n.lte(upperBound);
        return n;
      })));
    } catch (NumberFormatException e) {
      // Fall back to term range for non-numeric values
      return Query.of(q -> q.range(r -> r.term(t -> {
        t.field(esField);
        if (!RANGE_WILDCARD.equals(values[0])) t.gte(values[0]);
        if (!RANGE_WILDCARD.equals(values[1])) t.lte(values[1]);
        return t;
      })));
    }
  }

  public static Query buildGeoShapeQuery(String wkt) {
    // ES9'da en kolay yol WKT string'i direkt kullanmak
    // Sadece normalizasyon yapalım (eski kod logic'ini korumak için)
    Geometry geometry;
    try {
      geometry = new WKTReader().read(wkt);
    } catch (ParseException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }

    // Coordinate normalizasyonu yapıp WKT'yi yeniden oluşturalım
    String normalizedWkt = normalizeGeometryWkt(geometry);

    return Query.of(q -> q.geoShape(geoShape -> geoShape
      .field("coordinate")
      .shape(s -> s.shape(JsonData.fromJson(normalizedWkt)))
      //.relation(GeoShapeRelation.Within)
    ));
  }

  private static String normalizeGeometryWkt(Geometry geometry) {
    // Eski kodun logic'ini koruyarak normalizasyon yapalım
    String type = "LinearRing".equals(geometry.getGeometryType())
        ? "LINESTRING"
        : geometry.getGeometryType().toUpperCase();

    if ("POINT".equals(type)) {
      Point point = (Point) geometry;
      return String.format("POINT (%f %f)", point.getX(), point.getY());

    } else if ("LINESTRING".equals(type)) {
      LineString lineString = (LineString) geometry;
      Coordinate[] coords = normalizePolygonCoordinates(lineString.getCoordinates());
      StringBuilder wkt = new StringBuilder("LINESTRING (");
      for (int i = 0; i < coords.length; i++) {
        if (i > 0) wkt.append(", ");
        wkt.append(String.format("%f %f", coords[i].x, coords[i].y));
      }
      wkt.append(")");
      return wkt.toString();

    } else if ("POLYGON".equals(type)) {
      Polygon polygon = (Polygon) geometry;
      return normalizePolygonWkt(polygon);

    } else if ("MULTIPOLYGON".equals(type)) {
      MultiPolygon multiPolygon = (MultiPolygon) geometry;
      StringBuilder wkt = new StringBuilder("MULTIPOLYGON (");
      for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
        if (i > 0) wkt.append(", ");
        Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
        wkt.append("(").append(normalizePolygonWkt(polygon).substring(8)).append(")"); // Remove "POLYGON " prefix
      }
      wkt.append(")");
      return wkt.toString();

    } else {
      throw new IllegalArgumentException(type + " shape is not supported");
    }
  }

  private static String normalizePolygonWkt(Polygon polygon) {
    StringBuilder wkt = new StringBuilder("POLYGON (");

    // Exterior ring
    Coordinate[] exteriorCoords = normalizePolygonCoordinates(polygon.getExteriorRing().getCoordinates());
    wkt.append("(");
    for (int i = 0; i < exteriorCoords.length; i++) {
      if (i > 0) wkt.append(", ");
      wkt.append(String.format("%f %f", exteriorCoords[i].x, exteriorCoords[i].y));
    }
    wkt.append(")");

    // Interior rings (holes)
    for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
      wkt.append(", (");
      Coordinate[] holeCoords = normalizePolygonCoordinates(polygon.getInteriorRingN(i).getCoordinates());
      for (int j = 0; j < holeCoords.length; j++) {
        if (j > 0) wkt.append(", ");
        wkt.append(String.format("%f %f", holeCoords[j].x, holeCoords[j].y));
      }
      wkt.append(")");
    }

    wkt.append(")");
    return wkt.toString();
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
