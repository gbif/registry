package org.gbif.registry.search.dataset;

import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.model.registry.search.DatasetSearchRequest;
import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.model.registry.search.DatasetSuggestRequest;
import org.gbif.api.model.registry.search.DatasetSuggestResult;
import org.gbif.api.service.registry.DatasetSearchService;
import org.gbif.registry.search.dataset.indexing.es.EsClient;
import org.gbif.registry.search.dataset.search.DatasetEsResponseParser;
import org.gbif.registry.search.dataset.search.common.EsSearchRequestBuilder;

import java.io.IOException;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DatasetSearchServiceEs implements DatasetSearchService {

  private static final int DEFAULT_SUGGEST_LIMIT = 10;
  private static final int MAX_SUGGEST_LIMIT = 100;
  private static final DatasetEsResponseParser ES_RESPONSE_PARSER = DatasetEsResponseParser.create();

  private final RestHighLevelClient restHighLevelClient;
  private final String index;

  @Autowired
  public DatasetSearchServiceEs(@Qualifier("esIndex") String index, @Qualifier("esHost") String esHosts) {
    this.index = index;
    restHighLevelClient = EsClient.provideEsClient(esHosts.split(","));
  }

  @Override
  public SearchResponse<DatasetSearchResult, DatasetSearchParameter> search(DatasetSearchRequest datasetSearchRequest) {
    try {
      SearchRequest searchRequest = EsSearchRequestBuilder.buildSearchRequest(datasetSearchRequest, true, index);
      return ES_RESPONSE_PARSER.buildSearchResponse(restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT), datasetSearchRequest);
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
      SearchRequest searchRequest = EsSearchRequestBuilder.buildSuggestQuery(request.getQ(),
                                                                             DatasetSearchParameter.DATASET_TITLE,
                                                                             request.getLimit(),
                                                                             index);
      return ES_RESPONSE_PARSER.buildSuggestResponse(restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT), DatasetSearchParameter.DATASET_TITLE);
    } catch (IOException ex) {
      log.error("Error executing the search operation", ex);
      throw new RuntimeException(ex);
    }
  }

}

