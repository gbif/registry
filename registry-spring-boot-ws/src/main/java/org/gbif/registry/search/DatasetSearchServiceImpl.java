package org.gbif.registry.search;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.model.registry.search.DatasetSearchRequest;
import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.model.registry.search.DatasetSuggestRequest;
import org.gbif.api.model.registry.search.DatasetSuggestResult;
import org.gbif.api.service.registry.DatasetSearchService;
import org.gbif.common.search.SearchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Qualifier("datasetSearchService")
@Service
public class DatasetSearchServiceImpl implements DatasetSearchService {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetSearchServiceImpl.class);
  private static final int DEFAULT_SUGGEST_LIMIT = 10;
  private static final int MAX_SUGGEST_LIMIT = 100;

  /*
   * Solr server instance, this abstract type is used because it can hold instance of:
   * CommonsHttpSolrServer or EmbeddedSolrServer.
   */
  private final SolrClient solrClient;
  private final ResponseBuilder responseBuilder = new ResponseBuilder();
  private final SolrQueryBuilder queryBuilder = new SolrQueryBuilder();

  /**
   * Default constructor.
   */
  public DatasetSearchServiceImpl(SolrClient solrClient) {
    this.solrClient = solrClient;
  }

  /**
   * Issues a SolrQuery and converts the response to a SearchResponse object. Besides, the facets and paging
   * parameter and responses are handled in the request and response objects.
   *
   * @param request the request that contains the search parameters
   *
   * @return the SearchResponse of the search operation
   */
  @Override
  public SearchResponse<DatasetSearchResult, DatasetSearchParameter> search(DatasetSearchRequest request) {
    SolrQuery solrQuery = queryBuilder.build(request);

    QueryResponse solrResp = query(solrQuery);
    SearchResponse<DatasetSearchResult, DatasetSearchParameter> resp = responseBuilder.buildSearch(request, solrResp);

    return resp;
  }

  @Override
  public List<DatasetSuggestResult> suggest(DatasetSuggestRequest request) {
    // add defaults
    if (request.getLimit() < 1 || request.getLimit() > MAX_SUGGEST_LIMIT) {
      LOG.info("Suggest request with limit {} found. Reset to default {}", request.getLimit(), DEFAULT_SUGGEST_LIMIT);
      request.setLimit(DEFAULT_SUGGEST_LIMIT);
    }
    if (request.getOffset() > 0) {
      LOG.debug("Suggest request with offset {} found", request.getOffset());
    }

    // execute
    SolrQuery solrQuery = queryBuilder.build(request);
    QueryResponse response = query(solrQuery);
    return responseBuilder.buildSuggest(response);
  }

  private QueryResponse query(SolrQuery query) {
    try {
      // Executes the search operation in Solr
      LOG.debug("Solr query executed: {}", query);
      return solrClient.query(query);

    } catch (SolrServerException e) {
      if (e.getRootCause() instanceof IllegalArgumentException) {
        LOG.error("Bad query", e);
        throw (IllegalArgumentException) e.getRootCause();
      } else {
        LOG.error("Error querying solr {}", query, e);
        throw new SearchException(e);
      }

    } catch (IOException e) {
      LOG.error("Error querying solr {}", query, e);
      throw new SearchException(e);
    }
  }
}
