package org.gbif.registry.search.dataset.search;

import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.model.registry.search.DatasetSuggestResult;
import org.gbif.registry.search.dataset.search.common.EsResponseParser;

public class DatasetEsResponseParser extends EsResponseParser<DatasetSearchResult, DatasetSuggestResult, DatasetSearchParameter> {

  private DatasetEsResponseParser() {
    super(new DatasetSearchResultConverter(), new DatasetEsFieldMapper());
  }

  public static DatasetEsResponseParser create() {
    return new DatasetEsResponseParser();
  }

}
