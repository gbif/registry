package org.gbif.registry.collections.sync.ih;

import java.util.List;

import lombok.Data;
import lombok.SneakyThrows;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

import static org.gbif.registry.collections.sync.http.SyncCall.syncCall;

/** Lightweight IndexHerbariorum client. */
public class IHHttpClient {

  private final API api;

  private IHHttpClient(String ihWsUrl) {
    Retrofit retrofit =
      new Retrofit.Builder()
        .baseUrl(ihWsUrl)
        .addConverterFactory(JacksonConverterFactory.create())
        .build();
    api = retrofit.create(API.class);
  }

  public static IHHttpClient create(String ihWsUrl) {
    return new IHHttpClient(ihWsUrl);
  }

  @SneakyThrows
  public List<IHInstitution> getInstitutions() {
    return syncCall(api.listInstitutions()).getData();
  }

  @SneakyThrows
  public List<IHStaff> getStaffByInstitution(String institutionCode) {
    return syncCall(api.listStaff(institutionCode)).getData();
  }

  private interface API {
    @GET("institutions")
    Call<InstitutionWrapper> listInstitutions();

    @GET("staff/search")
    Call<StaffWrapper> listStaff(@Query("code") String institutionCode);
  }

  @Data
  private static class InstitutionWrapper {
    private IHMetadata meta;
    private List<IHInstitution> data;
  }

  @Data
  private static class StaffWrapper {
    private IHMetadata meta;
    private List<IHStaff> data;
  }
}
