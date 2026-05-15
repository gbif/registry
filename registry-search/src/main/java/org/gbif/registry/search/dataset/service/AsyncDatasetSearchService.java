package org.gbif.registry.search.dataset.service;

import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.model.registry.search.DatasetSearchRequest;
import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.model.registry.search.DatasetSuggestRequest;
import org.gbif.api.model.registry.search.DatasetSuggestResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Async search interface for dataset search backed by Elasticsearch. */
public interface AsyncDatasetSearchService {

  CompletableFuture<SearchResponse<DatasetSearchResult, DatasetSearchParameter>> searchAsync(
      DatasetSearchRequest datasetSearchRequest);

  CompletableFuture<List<DatasetSuggestResult>> suggestAsync(DatasetSuggestRequest datasetSuggestRequest);
}

