package org.gbif.registry.search.dataset.search;

import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.model.registry.search.DatasetSuggestResult;
import org.gbif.registry.search.dataset.search.common.EsResponseParser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DatasetEsResponseParser extends EsResponseParser<DatasetSearchResult, DatasetSuggestResult, DatasetSearchParameter> {

  @Autowired
  private DatasetEsResponseParser(DatasetSearchResultConverter datasetSearchResultConverter) {
    super(datasetSearchResultConverter, new DatasetEsFieldMapper());
  }

  public static DatasetEsResponseParser create(DatasetSearchResultConverter datasetSearchResultConverter) {
    return new DatasetEsResponseParser(datasetSearchResultConverter);
  }

}
