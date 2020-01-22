package org.gbif.registry.search;

import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.model.registry.search.DatasetSearchRequest;
import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.model.registry.search.DatasetSuggestRequest;
import org.gbif.api.model.registry.search.DatasetSuggestResult;
import org.gbif.api.service.registry.DatasetSearchService;

import java.util.Collections;
import java.util.List;

public class DatasetSearchServiceStub implements DatasetSearchService {

  @Override
  public SearchResponse<DatasetSearchResult, DatasetSearchParameter> search(DatasetSearchRequest datasetSearchRequest) {
    SearchResponse<DatasetSearchResult, DatasetSearchParameter> response = new SearchResponse<>(datasetSearchRequest);
    response.setResults(Collections.emptyList());
    return response;
  }

  @Override
  public List<DatasetSuggestResult> suggest(DatasetSuggestRequest datasetSuggestRequest) {
    return Collections.emptyList();
  }
}
