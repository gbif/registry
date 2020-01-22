package org.gbif.registry.search.dataset.search;

import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.model.registry.search.DatasetSuggestResult;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetSubtype;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.License;
import org.gbif.registry.search.dataset.search.common.SearchResultConverter;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.gbif.registry.search.dataset.indexing.es.EsQueryUtils.STRING_TO_DATE;

@Slf4j
@Component
public class DatasetSearchResultConverter implements SearchResultConverter<DatasetSearchResult, DatasetSuggestResult> {

  private static final Pattern NESTED_PATTERN = Pattern.compile("^\\w+(\\.\\w+)+$");
  private static final Predicate<String> IS_NESTED = s -> NESTED_PATTERN.matcher(s).find();

  private RestHighLevelClient restHighLevelClient;
  private String index;

  @Autowired
  public DatasetSearchResultConverter(RestHighLevelClient restHighLevelClient, @Value("${elasticsearch.index}") String index) {
    this.restHighLevelClient = restHighLevelClient;
    this.index = index;
  }

  @Override
  public DatasetSearchResult toSearchResult(SearchHit hit) {

    DatasetSearchResult d = new DatasetSearchResult();
    Map<String,Object> fields = hit.getSourceAsMap();
    d.setKey(UUID.fromString(hit.getId()));
    getStringValue(fields,"title").ifPresent(d::setTitle);
    getDatasetTypeValue(fields, "type").ifPresent(d::setType);
    getDatasetSubTypeValue(fields, "subtype").ifPresent(d::setSubtype);
    getStringValue(fields, "title").ifPresent(d::setTitle);
    getStringValue(fields, "description").ifPresent(d::setDescription);
    getUuidValue(fields, "publishingOrganizationKey").ifPresent(d::setPublishingOrganizationKey);
    getStringValue(fields, "publishingOrganizationTitle").ifPresent(d::setPublishingOrganizationTitle);

    getUuidValue(fields, "hostingOrganizationKey").ifPresent(d::setHostingOrganizationKey);
    getStringValue(fields, "hostingOrganizationTitle").ifPresent(d::setHostingOrganizationTitle);

    getCountryValue(fields, "publishingCountry").ifPresent(d::setPublishingCountry);
    getLicenceValue(fields, "license").ifPresent(d::setLicense);
    getStringValue(fields, "projectId").ifPresent(d::setProjectIdentifier);

    if (Objects.nonNull(d.getType())) {
      if (DatasetType.CHECKLIST == d.getType()) {
        getIntValue(fields, "nameUsagesCount").ifPresent(d::setRecordCount);
      } else {
        getIntValue(fields, "occurrenceCount").ifPresent(d::setRecordCount);
      }
    }

    getListValue(fields, "keyword").ifPresent(d::setKeywords);

    getIntegerValue(fields, "decade").ifPresent(d::setDecades);
    getCountryListValue(fields, "countryCoverage").ifPresent(d::setCountryCoverage);

    return d;

  }

  @Override
  public DatasetSuggestResult toSearchSuggestResult(SearchHit hit) {
    try {
      DatasetSuggestResult d = new DatasetSuggestResult();
      d.setKey(UUID.fromString(hit.getId()));
      SearchSourceBuilder searchById = new SearchSourceBuilder().size(1).query(QueryBuilders.idsQuery().addIds(hit.getId()));

      //Get the document data since the suggest response do not contain all the data
      SearchResponse searchByIdResponse = restHighLevelClient.search(new SearchRequest().source(searchById).indices(index), RequestOptions.DEFAULT);
      Map<String, Object> fields = hit.getSourceAsMap();
      if(searchByIdResponse.getHits().getTotalHits() > 0) {
        fields = searchByIdResponse.getHits().getAt(0).getSourceAsMap();
      }
      getStringValue(fields, "title").ifPresent(d::setTitle);
      getDatasetTypeValue(fields, "type").ifPresent(d::setType);
      getDatasetSubTypeValue(fields, "subtype").ifPresent(d::setSubtype);
      getStringValue(fields, "description").ifPresent(d::setDescription);

      return d;
    } catch (IOException ex) {
      log.error("Error converting suggestion", ex);
      throw new RuntimeException(ex);
    }
  }


  private static Optional<License> getLicenceValue(Map<String, Object> fields, String esField) {
    return getValue(fields, esField, value -> VocabularyUtils.lookupEnum(value, License.class));
  }

  private static Optional<Country> getCountryValue(Map<String, Object> fields, String esField) {
    return getValue(fields, esField, Country::fromIsoCode);
  }

  private static Optional<DatasetType> getDatasetTypeValue(Map<String, Object> fields, String esField) {
    return getValue(fields, esField, value -> VocabularyUtils.lookupEnum(value, DatasetType.class));
  }

  private static Optional<DatasetSubtype> getDatasetSubTypeValue(Map<String, Object> fields, String esField) {
    return getValue(fields, esField, value -> VocabularyUtils.lookupEnum(value, DatasetSubtype.class));
  }

  private static Optional<Set<Country>> getCountryListValue(Map<String, Object> fields, String esField) {
    return Optional.ofNullable(fields.get(esField))
      .map(v -> (List<String>) v)
      .filter(v -> !v.isEmpty())
      .map(v -> v.stream().map(Country::fromIsoCode).collect(Collectors.toSet()));

  }

  private static Optional<UUID> getUuidValue(Map<String, Object> fields, String esField) {
    return getValue(fields, esField, UUID::fromString);
  }

  private static Optional<String> getStringValue(Map<String, Object> fields, String esField) {
    return getValue(fields, esField, Function.identity());
  }

  private static Optional<Integer> getIntValue(Map<String, Object> fields, String esField) {
    return getValue(fields, esField, Integer::valueOf);
  }

  private static Optional<Double> getDoubleValue(Map<String, Object> fields, String esField) {
    return getValue(fields, esField, Double::valueOf);
  }

  private static Optional<Date> getDateValue(Map<String, Object> fields, String esField) {
    return getValue(fields, esField, STRING_TO_DATE);
  }

  private static Optional<List<String>> getListValue(Map<String, Object> fields, String esField) {
    return Optional.ofNullable(fields.get(esField))
      .map(v -> (List<String>) v)
      .filter(v -> !v.isEmpty());
  }

  private static Optional<List<Integer>> getIntegerValue(Map<String, Object> fields, String esField) {
    return Optional.ofNullable(fields.get(esField))
      .map(v -> (List<Integer>) v)
      .filter(v -> !v.isEmpty());
  }

  private static Optional<List<Map<String, Object>>> getObjectsListValue(Map<String, Object> fields, String esField) {
    return Optional.ofNullable(fields.get(esField))
      .map(v -> (List<Map<String, Object>>) v)
      .filter(v -> !v.isEmpty());
  }
  private static <T> Optional<T> getValue(Map<String, Object> fields, String esField, Function<String, T> mapper) {
    String fieldName = esField;
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

  private static <T> Optional<T> extractValue(Map<String, Object> fields, String fieldName, Function<String, T> mapper) {
    return Optional.ofNullable(fields.get(fieldName)).map(String::valueOf).filter(v -> !v.isEmpty())
      .map(v -> {
        try {
          return mapper.apply(v);
        } catch (Exception ex) {
          log.error("Error extracting field {} with value {}", fieldName, v);
          return null;
        }
      });
  }
}
