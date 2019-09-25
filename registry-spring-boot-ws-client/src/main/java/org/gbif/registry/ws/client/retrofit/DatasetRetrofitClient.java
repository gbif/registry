package org.gbif.registry.ws.client.retrofit;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.crawler.DatasetProcessStatus;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.model.registry.Network;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DatasetRetrofitClient extends BaseNetworkEntityRetrofitClient<Dataset> {

  @GET("dataset/{key}")
  @Override
  Call<Dataset> get(@Path("key") UUID key);

  @POST("dataset")
  @Override
  Call<UUID> create(@Body Dataset entity);

  @DELETE("dataset/{key}")
  @Override
  Call<Void> delete(@Path("key") UUID key);

  @GET("dataset")
  Call<PagingResponse<Dataset>> list(@QueryMap Map<String, String> options);

  @GET("dataset/{key}/document")
  Call<InputStream> getMetadataDocument(@Path("key") UUID datasetKey);

  @POST("dataset/{key}/document")
  Call<Metadata> insertMetadata(@Path("key") UUID datasetKey);

  @GET("dataset/{key}/constituents")
  Call<PagingResponse<Dataset>> listConstituents(@Path("key") UUID datasetKey,
                                                 @Query("limit") int limit,
                                                 @Query("offset") long offset);

  @GET("dataset/{key}/networks")
  Call<List<Network>> listNetworks(@Path("key") UUID datasetKey);

  @GET("dataset/constituents")
  Call<PagingResponse<Dataset>> listConstituents(@Query("limit") int limit,
                                                 @Query("offset") long offset);

  @GET("dataset/{key}/metadata")
  Call<List<Metadata>> listMetadata(@Path("key") UUID datasetKey, @Query("type") String type);

  @GET("dataset/metadata/{key}")
  Call<Metadata> getMetadata(@Path("key") int metadataKey);

  @GET("dataset/metadata/{key}/document")
  Call<InputStream> getMetadataDocument(@Path("key") int metadataKey);

  @DELETE("dataset/metadata/{key}")
  Call<Void> deleteMetadata(@Path("key") int metadataKey);

  @GET("dataset/deleted")
  Call<PagingResponse<Dataset>> listDeleted(@Query("limit") int limit, @Query("offset") long offset);

  @GET("dataset/duplicate")
  Call<PagingResponse<Dataset>> listDuplicates(@Query("limit") int limit, @Query("offset") long offset);

  @GET("dataset/withNoEndpoint")
  Call<PagingResponse<Dataset>> listDatasetsWithNoEndpoint(@Query("limit") int limit, @Query("offset") long offset);

  @GET("dataset/doi/{doi}")
  Call<PagingResponse<Dataset>> listByDOI(@Path("doi") String doi,
                                          @Query("limit") int limit,
                                          @Query("offset") long offset);

  @POST("dataset/{datasetKey}/process")
  Call<Void> createDatasetProcessStatus(@Path("datasetKey") UUID datasetKey,
                                        @Body DatasetProcessStatus datasetProcessStatus);

  @PUT("dataset/{datasetKey}/process/{attempt}")
  Call<Void> updateDatasetProcessStatus(@Path("datasetKey") UUID datasetKey,
                                        @Path("attempt") int attempt,
                                        @Body DatasetProcessStatus datasetProcessStatus);

  @GET("dataset/{datasetKey}/process/{attempt}")
  Call<DatasetProcessStatus> getDatasetProcessStatus(@Path("datasetKey") UUID datasetKey,
                                                     @Path("attempt") int attempt);

  @GET("dataset/process")
  Call<PagingResponse<DatasetProcessStatus>> listDatasetProcessStatus(@Query("limit") int limit,
                                                                      @Query("offset") long offset);

  @GET("dataset/process/aborted")
  Call<PagingResponse<DatasetProcessStatus>> listAbortedDatasetProcesses(@Query("limit") int limit,
                                                                         @Query("offset") long offset);

  @GET("dataset/{datasetKey}/process")
  Call<PagingResponse<DatasetProcessStatus>> listDatasetProcessStatus(@Path("datasetKey") UUID datasetKey,
                                                                      @Query("limit") int limit,
                                                                      @Query("offset") long offset);
}
