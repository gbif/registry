package org.gbif.registry.search.dataset.search;

import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.registry.search.dataset.search.common.EsFieldParameterMapper;

import com.google.common.collect.ImmutableBiMap;

public class DatasetEsFieldMapper implements EsFieldParameterMapper<DatasetSearchParameter> {

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



  public DatasetSearchParameter get(String esField) {
    return SEARCH_TO_ES_MAPPING.inverse().get(esField);
  }

  public String get(DatasetSearchParameter datasetSearchParameter) {
    return SEARCH_TO_ES_MAPPING.get(datasetSearchParameter);
  }


}
