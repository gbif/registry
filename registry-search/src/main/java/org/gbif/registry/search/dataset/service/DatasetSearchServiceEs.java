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

import com.fasterxml.jackson.databind.node.ObjectNode;

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

import java.io.IOException;
import java.util.List;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
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
  private final ElasticsearchClient elasticsearchClient;
  private final String index;

  private final EsSearchRequestBuilder<DatasetSearchParameter> esSearchRequestBuilder =
      new EsSearchRequestBuilder<>(new DatasetEsFieldMapper());

  @Autowired
  public DatasetSearchServiceEs(
      @Value("${elasticsearch.registry.index}") String index,
      ElasticsearchClient elasticsearchClient) {
    this.index = index;
    this.elasticsearchClient = elasticsearchClient;
  }

  @Override
  public SearchResponse<DatasetSearchResult, DatasetSearchParameter> search(
      DatasetSearchRequest datasetSearchRequest) {
    try {
      SearchRequest searchRequest =
          esSearchRequestBuilder.buildSearchRequest(datasetSearchRequest, true, index);
      co.elastic.clients.elasticsearch.core.SearchResponse<ObjectNode> response =
          elasticsearchClient.search(searchRequest, ObjectNode.class);
      return esResponseParser.buildSearchResponse(response, datasetSearchRequest);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public List<DatasetSuggestResult> suggest(DatasetSuggestRequest datasetSuggestRequest) {
    try {
      int limit = datasetSuggestRequest.getLimit();
      if (limit <= 0) {
        limit = DEFAULT_SUGGEST_LIMIT;
      } else if (limit > MAX_SUGGEST_LIMIT) {
        limit = MAX_SUGGEST_LIMIT;
      }

      // Create a copy of the request with the validated limit
      DatasetSuggestRequest modifiedRequest = new DatasetSuggestRequest();
      modifiedRequest.setQ(datasetSuggestRequest.getQ());
      modifiedRequest.setLimit(limit);
      modifiedRequest.setOffset(datasetSuggestRequest.getOffset());
      modifiedRequest.setParameters(datasetSuggestRequest.getParameters());

      SearchRequest searchRequest =
          esSearchRequestBuilder.buildAutocompleteQuery(modifiedRequest, null, index);
      co.elastic.clients.elasticsearch.core.SearchResponse<String> response =
          elasticsearchClient.search(searchRequest, String.class);
      org.gbif.api.model.common.search.SearchResponse<DatasetSuggestResult, org.gbif.api.model.registry.search.DatasetSearchParameter> autocompleteResponse =
          esResponseParser.buildSearchAutocompleteResponse(response, modifiedRequest);
      return autocompleteResponse.getResults();
    } catch (IOException ex) {
      log.error("Error executing the search operation", ex);
      throw new RuntimeException(ex);
    }
  }
}
