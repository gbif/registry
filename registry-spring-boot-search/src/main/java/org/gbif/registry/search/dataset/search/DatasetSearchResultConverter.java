package org.gbif.registry.search.dataset.search;

import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.model.registry.search.DatasetSuggestResult;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetSubtype;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.License;
import org.gbif.registry.search.dataset.search.common.SearchResultConverter;

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

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.search.SearchHit;

import static org.gbif.registry.search.dataset.indexing.es.EsQueryUtils.STRING_TO_DATE;

@Slf4j
public class DatasetSearchResultConverter implements SearchResultConverter<DatasetSearchResult, DatasetSuggestResult> {

  private static final Pattern NESTED_PATTERN = Pattern.compile("^\\w+(\\.\\w+)+$");
  private static final Predicate<String> IS_NESTED = s -> NESTED_PATTERN.matcher(s).find();


  @Override
  public DatasetSearchResult toSearchResult(SearchHit hit) {

    DatasetSearchResult d = new DatasetSearchResult();

    getUuidValue(hit, "key").ifPresent(d::setKey);
    getStringValue(hit,"title").ifPresent(d::setTitle);
    getDatasetTypeValue(hit, "type").ifPresent(d::setType);
    getDatasetSubTypeValue(hit, "subtype").ifPresent(d::setSubtype);
    getStringValue(hit, "title").ifPresent(d::setTitle);
    getStringValue(hit, "description").ifPresent(d::setDescription);
    getUuidValue(hit, "publishingOrganizationKey").ifPresent(d::setPublishingOrganizationKey);
    getStringValue(hit, "publishingOrganizationTitle").ifPresent(d::setPublishingOrganizationTitle);

    getUuidValue(hit, "hostingOrganizationKey").ifPresent(d::setHostingOrganizationKey);
    getStringValue(hit, "hostingOrganizationTitle").ifPresent(d::setHostingOrganizationTitle);

    getCountryValue(hit, "publishingCountry").ifPresent(d::setPublishingCountry);
    getLicenceValue(hit, "license").ifPresent(d::setLicense);
    getStringValue(hit, "projectId").ifPresent(d::setProjectIdentifier);

    if (Objects.nonNull(d.getType())) {
      if (DatasetType.CHECKLIST == d.getType()) {
        getIntValue(hit, "nameUsagesCount").ifPresent(d::setRecordCount);
      } else {
        getIntValue(hit, "occurrenceCount").ifPresent(d::setRecordCount);
      }
    }

    getListValue(hit, "keyword").ifPresent(d::setKeywords);

    getIntegerValue(hit, "decade").ifPresent(d::setDecades);
    getCountryListValue(hit, "countryCoverage").ifPresent(d::setCountryCoverage);

    return d;

  }

  @Override
  public DatasetSuggestResult toSearchSuggestResult(SearchHit hit) {
    DatasetSuggestResult d = new DatasetSuggestResult();

    getUuidValue(hit, "key").ifPresent(d::setKey);
    getStringValue(hit,"title").ifPresent(d::setTitle);
    getDatasetTypeValue(hit, "type").ifPresent(d::setType);
    getDatasetSubTypeValue(hit, "subtype").ifPresent(d::setSubtype);
    getStringValue(hit, "description").ifPresent(d::setDescription);

    return d;
  }


  private static Optional<License> getLicenceValue(SearchHit hit, String esField) {
    return getValue(hit, esField, value -> VocabularyUtils.lookupEnum(value, License.class));
  }

  private static Optional<Country> getCountryValue(SearchHit hit, String esField) {
    return getValue(hit, esField, Country::fromIsoCode);
  }

  private static Optional<DatasetType> getDatasetTypeValue(SearchHit hit, String esField) {
    return getValue(hit, esField, value -> VocabularyUtils.lookupEnum(value, DatasetType.class));
  }

  private static Optional<DatasetSubtype> getDatasetSubTypeValue(SearchHit hit, String esField) {
    return getValue(hit, esField, value -> VocabularyUtils.lookupEnum(value, DatasetSubtype.class));
  }

  private static Optional<Set<Country>> getCountryListValue(SearchHit hit, String esField) {
    return Optional.ofNullable(hit.getSourceAsMap().get(esField))
      .map(v -> (List<String>) v)
      .filter(v -> !v.isEmpty())
      .map(v -> v.stream().map(Country::fromIsoCode).collect(Collectors.toSet()));

  }

  private static Optional<UUID> getUuidValue(SearchHit hit, String esField) {
    return getValue(hit, esField, UUID::fromString);
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

  private static Optional<List<Integer>> getIntegerValue(SearchHit hit, String esField) {
    return Optional.ofNullable(hit.getSourceAsMap().get(esField))
      .map(v -> (List<Integer>) v)
      .filter(v -> !v.isEmpty());
  }

  private static Optional<List<Map<String, Object>>> getObjectsListValue(SearchHit hit, String esField) {
    return Optional.ofNullable(hit.getSourceAsMap().get(esField))
      .map(v -> (List<Map<String, Object>>) v)
      .filter(v -> !v.isEmpty());
  }
  private static <T> Optional<T> getValue(SearchHit hit, String esField, Function<String, T> mapper) {
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
