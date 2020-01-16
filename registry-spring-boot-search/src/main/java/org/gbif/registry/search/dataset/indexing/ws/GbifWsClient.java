package org.gbif.registry.search.dataset.indexing.ws;

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.common.search.FacetedSearchRequest;
import org.gbif.api.model.common.search.SearchParameter;
import org.gbif.api.model.common.search.SearchRequest;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.occurrence.Occurrence;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.model.occurrence.search.OccurrenceSearchRequest;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteStreams;
import lombok.SneakyThrows;
import okhttp3.ResponseBody;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import static org.gbif.registry.search.dataset.indexing.ws.WebserviceParameter.*;

/**
 * Retrofit {@link GbifApiService} client.
 */
public class GbifWsClient {

  //Uses a cache for installations to avoid too many external calls
  Cache<String, Installation> installationCache = Cache2kBuilder.of(String.class, Installation.class)
                                                    .eternal(true).disableStatistics(true).permitNullValues(true)
                                                    .loader(this::loadInstallation).build();

  //Uses a cache for organizations to avoid too many external calls
  Cache<String, Organization> organizationCache = Cache2kBuilder.of(String.class, Organization.class)
                                                    .eternal(true).disableStatistics(true).permitNullValues(true)
                                                    .loader(this::loadOrganization).build();

  private final GbifApiService gbifApiService;


  private GbifWsClient(GbifApiService gbifApiService) {
    this.gbifApiService = gbifApiService;
  }

  /**
   * Factory method, only need the api base url.
   * @param apiBaseUrl GBIF Api base url, for example: https://api.gbif-dev.orf/v1/ .
   */
  public static GbifWsClient create(String apiBaseUrl) {
    ObjectMapper objectMapper = JacksonObjectMapper.get();
    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl(apiBaseUrl)
      .addConverterFactory(JacksonConverterFactory.create(objectMapper))
      .build();
    return new GbifWsClient(retrofit.create(GbifApiService.class));
  }

  public PagingResponse<Dataset> listDatasets(PagingRequest pagingRequest) {
    Map<String,String> params = new HashMap<>();
    params.put("offset", Long.toString(pagingRequest.getOffset()));
    params.put("limit", Long.toString(pagingRequest.getLimit()));
    return syncCallWithResponse(gbifApiService.listDatasets(params)).body();
  }

  public Installation getInstallation(String installationKey) {
    return installationCache.get(installationKey);
  }

  private Installation loadInstallation(String installationKey) {
    return syncCallWithResponse(gbifApiService.getInstallation(installationKey)).body();
  }

  public Organization getOrganization(String organizationKey) {
    return organizationCache.get(organizationKey);
  }

  private Organization loadOrganization(String organizationKey) {
    return syncCallWithResponse(gbifApiService.getOrganization(organizationKey)).body();
  }

  public InputStream getMetadataDocument(UUID datasetKey) {
    try {
      Response<ResponseBody> response = syncCallWithResponse(gbifApiService.getMetadataDocument(datasetKey.toString()));
      if (response.isSuccessful() && response.body().contentLength() > 0) {
        return new ByteArrayInputStream(ByteStreams.toByteArray(response.body().byteStream()));
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    return null;
  }

  public Long getDatasetRecordCount(String datasetKey) {
    return syncCallWithResponse(gbifApiService.getDatasetRecordCount(datasetKey)).body();
  }

  public Long getOccurrenceRecordCount() {
    return syncCallWithResponse(gbifApiService.getOccurrenceRecordCount()).body();
  }

  public DatasetMetrics getDatasetSpeciesMetrics(String datasetKey) {
    return syncCallWithResponse(gbifApiService.getDatasetSpeciesMetrics(datasetKey)).body();
  }

  public SearchResponse<NameUsage, NameUsageSearchParameter> speciesSearch(NameUsageSearchRequest searchRequest) {
    return syncCallWithResponse(gbifApiService.speciesSearch(getParameterFromSearchRequest(searchRequest))).body();
  }

  public SearchResponse<Occurrence, OccurrenceSearchParameter> occurrenceSearch(OccurrenceSearchRequest searchRequest) {
    return syncCallWithResponse(gbifApiService.occurrenceSearch(getParameterFromSearchRequest(searchRequest))).body();
  }

  protected <P extends SearchParameter,R extends FacetedSearchRequest<P>> ProxyRetrofitQueryMap getParameterFromRequest(@Nullable R searchRequest) {
    // The searchRequest is transformed in a parameter map
    ProxyRetrofitQueryMap parameters = getParameterFromRequest(searchRequest);

    if (searchRequest != null) {
      parameters.put(PARAM_FACET_MULTISELECT, Boolean.toString(searchRequest.isMultiSelectFacets()));
      if (searchRequest.getFacetMinCount() != null) {
        parameters.put(PARAM_FACET_MINCOUNT, Integer.toString(searchRequest.getFacetMinCount()));
      }
      if (searchRequest.getFacetLimit() != null) {
        parameters.put(PARAM_FACET_LIMIT, Integer.toString(searchRequest.getFacetLimit()));
      }
      if (searchRequest.getFacetOffset() != null) {
        parameters.put(PARAM_FACET_OFFSET, Integer.toString(searchRequest.getFacetOffset()));
      }
      if (searchRequest.getFacets() != null) {
        for (P facet : searchRequest.getFacets()) {
          parameters.put(PARAM_FACET, facet.name());
          Pageable facetPage = searchRequest.getFacetPage(facet);
          if (facetPage != null) {
            parameters.put(facet.name() + '.' + PARAM_FACET_OFFSET, Long.toString(facetPage.getOffset()));
            parameters.put(facet.name() + '.' + PARAM_FACET_LIMIT, Long.toString(facetPage.getLimit()));
          }
        }
      }
    }

    return parameters;
  }

  protected <P extends SearchParameter> ProxyRetrofitQueryMap getParameterFromSearchRequest(@Nullable SearchRequest<P> searchRequest) {

    // The searchRequest is transformed in a parameter map
    ProxyRetrofitQueryMap parameters = new ProxyRetrofitQueryMap();

    if (searchRequest == null) {
      parameters.put(PARAM_QUERY_STRING, DEFAULT_SEARCH_PARAM_VALUE);

    } else {
      String searchParamValue = searchRequest.getQ();
      if (Strings.isNullOrEmpty(searchParamValue)) {
        searchParamValue = DEFAULT_SEARCH_PARAM_VALUE;
      }
      parameters.put(PARAM_QUERY_STRING, searchParamValue);
      parameters.put(PARAM_HIGHLIGHT, Boolean.toString(searchRequest.isHighlight()));
      parameters.put(PARAM_SPELLCHECK, Boolean.toString(searchRequest.isSpellCheck()));
      parameters.put(PARAM_SPELLCHECK_COUNT,Integer.toString(searchRequest.getSpellCheckCount()));

      Multimap<P, String> requestParameters = searchRequest.getParameters();
      if (requestParameters != null) {
        for (P param : requestParameters.keySet()) {
          parameters.put(param.name(), Lists.newArrayList(requestParameters.get(param)));
        }
      }
    }
    return parameters;
  }

  public class ProxyRetrofitQueryMap extends HashMap<String, Object> {
    public ProxyRetrofitQueryMap() {
      super(new HashMap<>());
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
      Set<Entry<String, Object>> originSet = super.entrySet();
      Set<Entry<String, Object>> newSet = new HashSet<>();

      for (Entry<String, Object> entry : originSet) {
        String entryKey = entry.getKey();
        if (entryKey == null) {
          throw new IllegalArgumentException("Query map contained null key.");
        }
        Object entryValue = entry.getValue();
        if (entryValue == null) {
          throw new IllegalArgumentException(
            "Query map contained null value for key '" + entryKey + "'.");
        }
        else if(entryValue instanceof List) {
          for(Object arrayValue:(List)entryValue)  {
            if (arrayValue != null) { // Skip null values
              Entry<String, Object> newEntry = new AbstractMap.SimpleEntry<>(entryKey, arrayValue);
              newSet.add(newEntry);
            }
          }
        }
        else {
          Entry<String, Object> newEntry = new AbstractMap.SimpleEntry<>(entryKey, entryValue);
          newSet.add(newEntry);
        }
      }
      return newSet;
    }
  }

  /**
   * Performs a synchronous call to {@link Call} instance.
   *
   * @param call to be executed
   * @param <T>  content of the response object
   * @return {@link Response} with content,
   * throws a {@link RuntimeException} when IOException was thrown from execute method
   */
  @SneakyThrows
  private static <T> Response<T> syncCallWithResponse(Call<T> call) {
     return call.execute();
  }

}
