package org.gbif.registry.search.dataset.search;

import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.registry.search.dataset.search.common.EsFieldMapper;

import com.google.common.collect.ImmutableBiMap;

public class DatasetEsFieldMapper implements EsFieldMapper<DatasetSearchParameter> {

  private static final ImmutableBiMap<DatasetSearchParameter,String> SEARCH_TO_ES_MAPPING = ImmutableBiMap.<DatasetSearchParameter,String>builder()
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
    .put(DatasetSearchParameter.DATASET_TITLE, "title")
    .build();

  private static final String[] EXCLUDE_FIELDS = new String[]{"all","taxonKey"};

  private static final String[] DATASET_TITLE_SUGGEST_FIELDS = new String[]{"title", "type", "subtype", "description"};



  @Override
  public DatasetSearchParameter get(String esField) {
    return SEARCH_TO_ES_MAPPING.inverse().get(esField);
  }

  @Override
  public String get(DatasetSearchParameter datasetSearchParameter) {
    return SEARCH_TO_ES_MAPPING.get(datasetSearchParameter);
  }

  @Override
  public String[] excludeFields() {
    return EXCLUDE_FIELDS;
  }

  @Override
  public String[] includeSuggestFields(DatasetSearchParameter searchParameter) {
    if (DatasetSearchParameter.DATASET_TITLE == searchParameter) {
      return DATASET_TITLE_SUGGEST_FIELDS;
    }
    return new String[]{SEARCH_TO_ES_MAPPING.get(searchParameter)};
  }
}
