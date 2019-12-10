package org.gbif.registry.ws.client.retrofit;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OccurrenceDownloadRetrofitClient {

  @POST("occurrence/download")
  Call<Void> create(@Body Download occurrenceDownload);

  @GET("occurrence/download/{key}")
  Call<Download> get(@Path("key") String key);

  @GET("occurrence/download/{prefix}/{suffix}")
  Call<Download> getByDoi(@Path("prefix") String prefix, @Path("suffix") String suffix);

  @GET("occurrence/download")
  Call<PagingResponse<Download>> list(@Query("status") List<String> status,
                                      @Query("limit") int limit,
                                      @Query("offset") long offset);

  @GET("occurrence/download/user/{user}")
  Call<PagingResponse<Download>> listByUser(@Path("user") String user,
                                            @Query("status") List<String> status,
                                            @Query("limit") int limit,
                                            @Query("offset") long offset);

  @PUT("occurrence/download/{key}")
  Call<Void> update(@Body Download download);

  @GET("occurrence/download/{key}/datasets")
  Call<PagingResponse<DatasetOccurrenceDownloadUsage>> listDatasetUsages(@Path("key") String downloadKey,
                                                                         @Query("limit") int limit,
                                                                         @Query("offset") long offset);

  @POST("occurrence/download/{key}/datasets")
  Call<Void> createUsages(@Path("key") String downloadKey, @Body Map<UUID, Long> datasetCitations);

  @GET("occurrence/download/statistics/downloadsByUserCountry")
  Call<Map<Integer, Map<Integer, Long>>> getDownloadsByUserCountry(@Query("fromDate") String fromDate,
                                                                   @Query("toDate") String toDate,
                                                                   @Query("userCountry") String userCountry);

  @GET("occurrence/download/statistics/downloadedRecordsByDataset")
  Call<Map<Integer, Map<Integer, Long>>> getDownloadedRecordsByDataset(@Query("fromDate") String fromDate,
                                                                       @Query("toDate") String toDate,
                                                                       @Query("publishingCountry") String publishingCountry,
                                                                       @Query("datasetKey") UUID datasetKey);
}
