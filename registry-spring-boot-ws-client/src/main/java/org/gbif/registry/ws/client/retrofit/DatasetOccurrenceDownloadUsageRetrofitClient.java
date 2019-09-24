package org.gbif.registry.ws.client.retrofit;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.DatasetOccurrenceDownloadUsage;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.UUID;

public interface DatasetOccurrenceDownloadUsageRetrofitClient {

  @GET("occurrence/download/dataset/{datasetKey}")
  Call<PagingResponse<DatasetOccurrenceDownloadUsage>> listByDataset(@Path("datasetKey") UUID datasetKey,
                                                                     @Query("limit") int limit,
                                                                     @Query("offset") long offset);
}
