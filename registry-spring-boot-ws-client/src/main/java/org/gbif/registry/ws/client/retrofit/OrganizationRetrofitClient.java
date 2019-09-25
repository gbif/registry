package org.gbif.registry.ws.client.retrofit;

import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.ConfirmationKeyParameter;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.search.KeyTitleResult;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OrganizationRetrofitClient extends BaseNetworkEntityRetrofitClient<Organization> {

  @POST("organization")
  @Override
  Call<UUID> create(@Body Organization organization);

  @GET("organization/{key}")
  @Override
  Call<Organization> get(@Path("key") UUID key);

  @DELETE("organization/{key}")
  @Override
  Call<Void> delete(@Path("key") UUID key);

  @POST("organization/{key}/endorsement")
  Call<Void> confirmEndorsement(@Path("key") UUID organizationKey, @Body ConfirmationKeyParameter confirmationKeyParameter);

  @PUT("organization/{key}")
  @Override
  Call<Void> update(@Path("key") UUID key, @Body Organization organization);

  @GET("organization/{key}/hostedDataset")
  Call<PagingResponse<Dataset>> hostedDatasets(@Path("key") UUID organizationKey,
                                               @Query("limit") int limit,
                                               @Query("offset") long offset);

  @GET("organization/{key}/publishedDataset")
  Call<PagingResponse<Dataset>> publishedDataset(@Path("key") UUID organizationKey,
                                                 @Query("limit") int limit,
                                                 @Query("offset") long offset);

  @GET("organization/count")
  Call<Integer> countOrganizations();

  @GET("organization/{key}/installation")
  Call<PagingResponse<Installation>> installations(@Path("key") UUID organizationKey,
                                                   @Query("limit") int limit,
                                                   @Query("offset") long offset);

  @GET("organization/listDeleted")
  Call<PagingResponse<Organization>> listDeleted(@Query("limit") int limit,
                                                 @Query("offset") long offset);

  @GET("organization/pending")
  Call<PagingResponse<Organization>> listPendingEndorsement(@Query("limit") int limit,
                                                            @Query("offset") long offset);

  @GET("organization/nonPublishing")
  Call<PagingResponse<Organization>> listNonPublishing(@Query("limit") int limit,
                                                       @Query("offset") long offset);

  @GET("organization/suggest")
  Call<List<KeyTitleResult>> suggest(@Query("q") String label);

  @GET("organization/{key}/password")
  Call<String> retrievePassword(@Path("key") UUID organizationKey);

  @GET("organization")
  Call<PagingResponse<Organization>> list(@QueryMap Map<String, String> options);
}
