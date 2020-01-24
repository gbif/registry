package org.gbif.registry.search.dataset.indexing.ws;

import org.gbif.api.model.checklistbank.DatasetMetrics;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.search.NameUsageSearchParameter;
import org.gbif.api.model.checklistbank.search.NameUsageSearchRequest;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import static org.gbif.registry.search.dataset.indexing.ws.SearchParameterProvider.getParameterFromFacetedRequest;

/**
 * Retrofit {@link GbifApiService} client.
 */
@Component
@Lazy
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


  /**
   * Factory method, only need the api base url.
   * @param apiBaseUrl GBIF Api base url, for example: https://api.gbif-dev.orf/v1/ .
   */
  @Autowired
  public GbifWsClient(@Value("${api.root.url}") String apiBaseUrl, @Qualifier("apiMapper") ObjectMapper objectMapper) {

    OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
      .connectTimeout(2, TimeUnit.MINUTES)
      .readTimeout(5, TimeUnit.MINUTES)
      .writeTimeout(1, TimeUnit.MINUTES)
      .build();
    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl(apiBaseUrl)
      .addConverterFactory(JacksonConverterFactory.create(objectMapper))
      .client(okHttpClient)
      .build();
    gbifApiService = retrofit.create(GbifApiService.class);
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
    return syncCallWithResponse(gbifApiService.speciesSearch(getParameterFromFacetedRequest(searchRequest))).body();
  }

  public SearchResponse<Occurrence, OccurrenceSearchParameter> occurrenceSearch(OccurrenceSearchRequest searchRequest) {
    return syncCallWithResponse(gbifApiService.occurrenceSearch(getParameterFromFacetedRequest(searchRequest))).body();
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
