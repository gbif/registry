package org.gbif.registry.search.dataset.indexing.es;


import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.model.occurrence.search.OccurrenceSearchRequest;
import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.vocabulary.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.elasticsearch.client.RequestOptions;

public class EsQueryUtils {

  private EsQueryUtils() {}

  // defaults
  private static final int DEFAULT_FACET_OFFSET = 0;
  private static final int DEFAULT_FACET_LIMIT = 10;

  // ES fields for queries
  public static final String SIZE = "size";
  public static final String FROM = "from";
  public static final String TO = "to";
  public static final String QUERY = "query";
  public static final String BOOL = "bool";
  public static final String MUST = "must";
  public static final String MATCH = "match";
  public static final String TERM = "term";
  public static final String TERMS = "terms";
  public static final String FILTER = "filter";
  public static final String SHOULD = "should";
  public static final String RANGE = "range";
  public static final String GTE = "gte";
  public static final String LTE = "lte";
  public static final String VALUE = "value";
  public static final String POST_FILTER = "post_filter";
  public static final String SUGGEST = "suggest";

  // Aggs
  public static final String FIELD = "field";
  public static final String AGGREGATIONS = "aggregations";
  public static final String AGGS = "aggs";
  public static final String PRECISION = "precision";
  public static final String GEOHASH_GRID = "geohash_grid";
  public static final String GEO_BOUNDS = "geo_bounds";
  public static final String GEO_BOUNDING_BOX = "geo_bounding_box";

  // geo_shape
  static final String GEO_SHAPE = "geo_shape";
  static final String COORDINATES = "coordinates";
  static final String TYPE = "type";
  static final String SHAPE = "shape";
  static final String RELATION = "relation";
  static final String WITHIN = "within";

  public static final String RANGE_SEPARATOR = ",";
  public static final String RANGE_WILDCARD = "*";

  private static final DateTimeFormatter FORMATTER =
    DateTimeFormatter.ofPattern(
      "[yyyy-MM-dd'T'HH:mm:ssXXX][yyyy-MM-dd'T'HH:mmXXX][yyyy-MM-dd'T'HH:mm:ss.SSS XXX][yyyy-MM-dd'T'HH:mm:ss.SSSXXX]"
      + "[yyyy-MM-dd'T'HH:mm:ss.SSSSSS][yyyy-MM-dd'T'HH:mm:ss.SSSSS][yyyy-MM-dd'T'HH:mm:ss.SSSS][yyyy-MM-dd'T'HH:mm:ss.SSS]"
      + "[yyyy-MM-dd'T'HH:mm:ss][yyyy-MM-dd'T'HH:mm:ss XXX][yyyy-MM-dd'T'HH:mm:ssXXX][yyyy-MM-dd'T'HH:mm:ss]"
      + "[yyyy-MM-dd'T'HH:mm][yyyy-MM-dd][yyyy-MM][yyyy]");

  static final Function<String, Date> STRING_TO_DATE =
    dateAsString -> {
      if (Strings.isNullOrEmpty(dateAsString)) {
        return null;
      }

      boolean firstYear = false;
      if (dateAsString.startsWith("0000")) {
        firstYear = true;
        dateAsString = dateAsString.replaceFirst("0000", "1970");
      }

      // parse string
      TemporalAccessor temporalAccessor = FORMATTER.parseBest(dateAsString,
                                                              ZonedDateTime::from,
                                                              LocalDateTime::from,
                                                              LocalDate::from,
                                                              YearMonth::from,
                                                              Year::from);
      Date dateParsed = null;
      if (temporalAccessor instanceof ZonedDateTime) {
        dateParsed = Date.from(((ZonedDateTime)temporalAccessor).toInstant());
      } else if (temporalAccessor instanceof LocalDateTime) {
        dateParsed = Date.from(((LocalDateTime)temporalAccessor).toInstant(ZoneOffset.UTC));
      } else if (temporalAccessor instanceof LocalDate) {
        dateParsed = Date.from((((LocalDate)temporalAccessor).atStartOfDay()).toInstant(ZoneOffset.UTC));
      } else if (temporalAccessor instanceof YearMonth) {
        dateParsed = Date.from((((YearMonth)temporalAccessor).atDay(1)).atStartOfDay().toInstant(ZoneOffset.UTC));
      } else if (temporalAccessor instanceof Year) {
        dateParsed = Date.from((((Year)temporalAccessor).atDay(1)).atStartOfDay().toInstant(ZoneOffset.UTC));
      }

      if (dateParsed != null && firstYear) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateParsed);
        cal.set(Calendar.YEAR, 1);
        return cal.getTime();
      }

      return dateParsed;
    };

  public static final Function<String, LocalDateTime> LOWER_BOUND_RANGE_PARSER =
    lowerBound -> {
      if (Strings.isNullOrEmpty(lowerBound) || RANGE_WILDCARD.equalsIgnoreCase(lowerBound)) {
        return null;
      }

      TemporalAccessor temporalAccessor =
        FORMATTER.parseBest(lowerBound, LocalDate::from, YearMonth::from, Year::from);

      if (temporalAccessor instanceof LocalDate) {
        return ((LocalDate) temporalAccessor).atTime(LocalTime.MIN);
      }

      if (temporalAccessor instanceof Year) {
        return Year.from(temporalAccessor).atMonth(Month.JANUARY).atDay(1).atTime(LocalTime.MIN);
      }

      if (temporalAccessor instanceof YearMonth) {
        return YearMonth.from(temporalAccessor).atDay(1).atTime(LocalTime.MIN);
      }

      return null;
    };

  public static final Function<String, LocalDateTime> UPPER_BOUND_RANGE_PARSER =
    upperBound -> {
      if (Strings.isNullOrEmpty(upperBound) || RANGE_WILDCARD.equalsIgnoreCase(upperBound)) {
        return null;
      }

      TemporalAccessor temporalAccessor =
        FORMATTER.parseBest(upperBound, LocalDate::from, YearMonth::from, Year::from);

      if (temporalAccessor instanceof LocalDate) {
        return ((LocalDate) temporalAccessor).atTime(LocalTime.MAX);
      }

      if (temporalAccessor instanceof Year) {
        return Year.from(temporalAccessor)
          .atMonth(Month.DECEMBER)
          .atEndOfMonth()
          .atTime(LocalTime.MAX);
      }

      if (temporalAccessor instanceof YearMonth) {
        return YearMonth.from(temporalAccessor).atEndOfMonth().atTime(LocalTime.MAX);
      }

      return null;
    };

  // functions
  public static final Supplier<RequestOptions> HEADERS =
    () -> {
      RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
      builder.addHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
      return builder.build();
    };

  public static final ImmutableMap<DatasetSearchParameter, String> SEARCH_TO_ES_MAPPING = ImmutableMap.<DatasetSearchParameter,String>builder()
    .put(DatasetSearchParameter.TAXON_KEY, "taxonKey")
    .put(DatasetSearchParameter.CONTINENT, "continent")
    .put(DatasetSearchParameter.COUNTRY, "country")
    .put(DatasetSearchParameter.PUBLISHING_COUNTRY, "publishingCountry")
    .put(DatasetSearchParameter.YEAR, "year")
    .put(DatasetSearchParameter.DECADE, "decade")
    .put(DatasetSearchParameter.HOSTING_ORG, "hostingOrganizationKey")
    .put(DatasetSearchParameter.KEYWORD, "keyword")
    .put(DatasetSearchParameter.LICENSE, "license")
    .put(DatasetSearchParameter.MODIFIED_DATE, "modified")
    .put(DatasetSearchParameter.PROJECT_ID, "project.identifier")
    .put(DatasetSearchParameter.PUBLISHING_ORG, "publishingOrganizationKey")
    .put(DatasetSearchParameter.RECORD_COUNT,"occurrenceCount")
    .put(DatasetSearchParameter.SUBTYPE, "subtype")
    .put(DatasetSearchParameter.TYPE, "type")
    .build();

  public static final Map<String, Integer> CARDINALITIES =
    ImmutableMap.<String, Integer>builder()
      .put("license", License.values().length)
      .put("country", Country.values().length)
      .put("publishingCountry", Country.values().length)
      .put("continent", Continent.values().length)
      .put("type", DatasetType.values().length)
      .put("subtype", DatasetSubtype.values().length)
      .build();



  public static final List<String> DATE_FIELDS =
    ImmutableList.of(
      "modified",
      "created",
      "pubDate");

  public static final Map<String, DatasetSearchParameter> ES_TO_SEARCH_MAPPING =
    new HashMap<>(SEARCH_TO_ES_MAPPING.size());

  static {
    for (Map.Entry<DatasetSearchParameter, String> paramField :
      SEARCH_TO_ES_MAPPING.entrySet()) {
      ES_TO_SEARCH_MAPPING.put(paramField.getValue(), paramField.getKey());
    }
  }

  public static <P extends SearchParameter> int extractFacetLimit(FacetedSearchRequest<P> request, P facet) {
    return Optional.ofNullable(request.getFacetPage(facet))
      .map(Pageable::getLimit)
      .orElse(request.getFacetLimit() != null ? request.getFacetLimit() : DEFAULT_FACET_LIMIT);
  }

  public static <P extends SearchParameter> int extractFacetOffset(FacetedSearchRequest<P> request, P facet) {
    return Optional.ofNullable(request.getFacetPage(facet))
      .map(v -> (int) v.getOffset())
      .orElse(request.getFacetOffset() != null ? request.getFacetOffset() : DEFAULT_FACET_OFFSET);
  }
}
