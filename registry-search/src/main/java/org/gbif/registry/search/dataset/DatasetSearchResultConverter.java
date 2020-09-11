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
package org.gbif.registry.search.dataset;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.model.registry.search.DatasetSuggestResult;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetSubtype;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.License;
import org.gbif.registry.search.dataset.common.SearchResultConverter;

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

import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;

import lombok.extern.slf4j.Slf4j;

import static org.gbif.registry.search.dataset.indexing.es.EsQueryUtils.STRING_TO_DATE;

@Slf4j
public class DatasetSearchResultConverter
    implements SearchResultConverter<DatasetSearchResult, DatasetSuggestResult> {

  private static final Pattern NESTED_PATTERN = Pattern.compile("^\\w+(\\.\\w+)+$");
  private static final Predicate<String> IS_NESTED = s -> NESTED_PATTERN.matcher(s).find();

  @Override
  public DatasetSearchResult toSearchResult(SearchHit hit) {

    DatasetSearchResult d = new DatasetSearchResult();
    Map<String, Object> fields = hit.getSourceAsMap();
    d.setKey(UUID.fromString(hit.getId()));
    getHighlightOrStringValue(fields, hit.getHighlightFields(), "title").ifPresent(d::setTitle);
    getDatasetTypeValue(fields, "type").ifPresent(d::setType);
    getDatasetSubTypeValue(fields, "subtype").ifPresent(d::setSubtype);
    getHighlightOrStringValue(fields, hit.getHighlightFields(), "description")
        .ifPresent(d::setDescription);
    getUuidValue(fields, "publishingOrganizationKey").ifPresent(d::setPublishingOrganizationKey);
    getHighlightOrStringValue(fields, hit.getHighlightFields(), "publishingOrganizationTitle")
        .ifPresent(d::setPublishingOrganizationTitle);
    getUuidValue(fields, "hostingOrganizationKey").ifPresent(d::setHostingOrganizationKey);
    getHighlightOrStringValue(fields, hit.getHighlightFields(), "hostingOrganizationTitle")
        .ifPresent(d::setHostingOrganizationTitle);

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
    getStringValue(fields, "doi").map(DOI::new).ifPresent(d::setDoi);

    return d;
  }

  @Override
  public DatasetSuggestResult toSearchSuggestResult(SearchHit hit) {
    DatasetSuggestResult d = new DatasetSuggestResult();
    d.setKey(UUID.fromString(hit.getId()));

    Map<String, Object> fields = hit.getSourceAsMap();

    getStringValue(fields, "title").ifPresent(d::setTitle);
    getDatasetTypeValue(fields, "type").ifPresent(d::setType);
    getDatasetSubTypeValue(fields, "subtype").ifPresent(d::setSubtype);
    getStringValue(fields, "description").ifPresent(d::setDescription);

    return d;
  }

  private static Optional<License> getLicenceValue(Map<String, Object> fields, String esField) {
    return getValue(fields, esField, value -> VocabularyUtils.lookupEnum(value, License.class));
  }

  private static Optional<Country> getCountryValue(Map<String, Object> fields, String esField) {
    return getValue(fields, esField, Country::fromIsoCode);
  }

  private static Optional<DatasetType> getDatasetTypeValue(
      Map<String, Object> fields, String esField) {
    return getValue(fields, esField, value -> VocabularyUtils.lookupEnum(value, DatasetType.class));
  }

  private static Optional<DatasetSubtype> getDatasetSubTypeValue(
      Map<String, Object> fields, String esField) {
    return getValue(
        fields, esField, value -> VocabularyUtils.lookupEnum(value, DatasetSubtype.class));
  }

  private static Optional<Set<Country>> getCountryListValue(
      Map<String, Object> fields, String esField) {
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

  private static Optional<String> getHighlightOrStringValue(
      Map<String, Object> fields, Map<String, HighlightField> hlFields, String esField) {
    Optional<String> fieldValue = getValue(fields, esField, Function.identity());
    if (Objects.nonNull(hlFields)) {
      Optional<String> hlValue =
          Optional.ofNullable(hlFields.get(esField))
              .map(hlField -> hlField.getFragments()[0].string());
      return hlValue.isPresent() ? hlValue : fieldValue;
    }
    return fieldValue;
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

  private static Optional<List<Integer>> getIntegerValue(
      Map<String, Object> fields, String esField) {
    return Optional.ofNullable(fields.get(esField))
        .map(v -> (List<Integer>) v)
        .filter(v -> !v.isEmpty());
  }

  private static Optional<List<Map<String, Object>>> getObjectsListValue(
      Map<String, Object> fields, String esField) {
    return Optional.ofNullable(fields.get(esField))
        .map(v -> (List<Map<String, Object>>) v)
        .filter(v -> !v.isEmpty());
  }

  private static <T> Optional<T> getValue(
      Map<String, Object> fields, String esField, Function<String, T> mapper) {
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
                log.error("Error extracting field {} with value {}", fieldName, v);
                return null;
              }
            });
  }
}
