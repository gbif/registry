package org.gbif.registry.stubs;

import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.model.registry.search.DatasetSearchRequest;
import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.model.registry.search.DatasetSuggestRequest;
import org.gbif.api.model.registry.search.DatasetSuggestResult;
import org.gbif.api.service.registry.DatasetSearchService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Qualifier("datasetSearchServiceStub")
public class SearchServiceStub implements DatasetSearchService {

  @Override
  public SearchResponse<DatasetSearchResult, DatasetSearchParameter> search(DatasetSearchRequest datasetSearchRequest) {
    return null;
  }

  @Override
  public List<DatasetSuggestResult> suggest(DatasetSuggestRequest datasetSuggestRequest) {
    return Collections.emptyList();
  }
}
