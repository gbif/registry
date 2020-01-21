package org.gbif.registry.search.dataset.service;

import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.model.registry.search.DatasetSearchRequest;
import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.model.registry.search.DatasetSuggestRequest;
import org.gbif.api.model.registry.search.DatasetSuggestResult;
import org.gbif.api.service.registry.DatasetSearchService;
import org.gbif.registry.search.dataset.search.DatasetEsFieldMapper;
import org.gbif.registry.search.dataset.search.DatasetEsResponseParser;
import org.gbif.registry.search.dataset.search.common.EsSearchRequestBuilder;

import java.io.IOException;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DatasetSearchServiceEs implements DatasetSearchService {

  private static final int DEFAULT_SUGGEST_LIMIT = 10;
  private static final int MAX_SUGGEST_LIMIT = 100;

  private final DatasetEsResponseParser esResponseParser;
  private final RestHighLevelClient restHighLevelClient;
  private final String index;

  private final EsSearchRequestBuilder<DatasetSearchParameter> esSearchRequestBuilder = new EsSearchRequestBuilder<>(new DatasetEsFieldMapper());

  @Autowired
  public DatasetSearchServiceEs(@Value("${esIndex}") String index, RestHighLevelClient restHighLevelClient, DatasetEsResponseParser esResponseParser) {
    this.index = index;
    this.esResponseParser = esResponseParser;
    this.restHighLevelClient = restHighLevelClient;
  }

  @Override
  public SearchResponse<DatasetSearchResult, DatasetSearchParameter> search(DatasetSearchRequest datasetSearchRequest) {
    try {
      SearchRequest searchRequest = esSearchRequestBuilder.buildSearchRequest(datasetSearchRequest, true, index);
      return esResponseParser.buildSearchResponse(restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT), datasetSearchRequest);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public List<DatasetSuggestResult> suggest(DatasetSuggestRequest request) {
    try {
      log.debug("ES request: {}", request);
      // add defaults
      if (request.getLimit() < 1 || request.getLimit() > MAX_SUGGEST_LIMIT) {
        log.info("Suggest request with limit {} found. Reset to default {}", request.getLimit(), DEFAULT_SUGGEST_LIMIT);
        request.setLimit(DEFAULT_SUGGEST_LIMIT);
      }
      if (request.getOffset() > 0) {
        log.debug("Suggest request with offset {} found", request.getOffset());
      }

      // execute
      SearchRequest searchRequest = esSearchRequestBuilder.buildSuggestQuery(request.getQ(),
                                                                             DatasetSearchParameter.DATASET_TITLE,
                                                                             request.getLimit(),
                                                                             index);
      return esResponseParser.buildSuggestResponse(restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT), DatasetSearchParameter.DATASET_TITLE);
    } catch (IOException ex) {
      log.error("Error executing the search operation", ex);
      throw new RuntimeException(ex);
    }
  }

}

