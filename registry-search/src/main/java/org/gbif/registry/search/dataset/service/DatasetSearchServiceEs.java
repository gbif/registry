/*
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
package org.gbif.registry.search.dataset.service;

import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.model.registry.search.DatasetSearchRequest;
import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.model.registry.search.DatasetSuggestRequest;
import org.gbif.api.model.registry.search.DatasetSuggestResult;
import org.gbif.api.service.registry.DatasetSearchService;
import org.gbif.registry.search.dataset.DatasetEsFieldMapper;
import org.gbif.registry.search.dataset.DatasetEsResponseParser;
import org.gbif.registry.search.dataset.common.EsSearchRequestBuilder;
import org.gbif.registry.search.dataset.indexing.Text2Embedding;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

import co.elastic.clients.elasticsearch._types.KnnQuery;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Qualifier("datasetSearchServiceEs")
public class DatasetSearchServiceEs implements DatasetSearchService {

  private static final int DEFAULT_SUGGEST_LIMIT = 10;
  private static final int MAX_SUGGEST_LIMIT = 100;

  private final DatasetEsResponseParser esResponseParser = DatasetEsResponseParser.create();
  private final RestHighLevelClient restHighLevelClient;

  private final ElasticsearchClient elasticsearchClient;
  private final String index;

  private final Text2Embedding text2Embedding;

  private final EsSearchRequestBuilder<DatasetSearchParameter> esSearchRequestBuilder =
      new EsSearchRequestBuilder<>(new DatasetEsFieldMapper());

  @Autowired
  public DatasetSearchServiceEs(
      @Value("${elasticsearch.registry.index}") String index,
      RestHighLevelClient restHighLevelClient,
      ElasticsearchClient elasticsearchClient) {
    this.index = index;
    this.restHighLevelClient = restHighLevelClient;
    this.elasticsearchClient = elasticsearchClient;
    this.text2Embedding = new Text2Embedding();
  }

  @Override
  public SearchResponse<DatasetSearchResult, DatasetSearchParameter> search(
      DatasetSearchRequest datasetSearchRequest) {
    try {
      SearchRequest searchRequest =
          esSearchRequestBuilder.buildSearchRequest(datasetSearchRequest, true, index);
      return esResponseParser.buildSearchResponse(
          restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT), datasetSearchRequest);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public  SearchResponse<DatasetSearchResult, DatasetSearchParameter> embeddingSearch(DatasetSearchRequest datasetSearchRequest) {

    co.elastic.clients.elasticsearch.core.SearchRequest searchRequest = new co.elastic.clients.elasticsearch.core.SearchRequest.Builder()
      .index(this.index)
      .size(datasetSearchRequest.getLimit())
      .from(Long.valueOf(datasetSearchRequest.getOffset()).intValue())
      .knn(new KnnQuery.Builder().k(10)
             .numCandidates(datasetSearchRequest.getLimit())
             .queryVector(Arrays.asList(text2Embedding.predict(datasetSearchRequest.getQ()))).build())
      .build();
    //elasticsearchClient.search(searchRequest);
    return null;
  }

  @Override
  public List<DatasetSuggestResult> suggest(DatasetSuggestRequest request) {
    try {
      log.debug("ES request: {}", request);
      // add defaults
      if (request.getLimit() < 1 || request.getLimit() > MAX_SUGGEST_LIMIT) {
        log.info(
            "Suggest request with limit {} found. Reset to default {}",
            request.getLimit(),
            DEFAULT_SUGGEST_LIMIT);
        request.setLimit(DEFAULT_SUGGEST_LIMIT);
      }
      if (request.getOffset() > 0) {
        log.debug("Suggest request with offset {} found", request.getOffset());
      }

      // execute
      SearchRequest searchRequest =
          esSearchRequestBuilder.buildAutocompleteQuery(
              request, DatasetSearchParameter.DATASET_TITLE, index);
      return esResponseParser
          .buildSearchAutocompleteResponse(
              restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT), request)
          .getResults();
    } catch (IOException ex) {
      log.error("Error executing the search operation", ex);
      throw new RuntimeException(ex);
    }
  }
}
