package org.gbif.registry.collections.sync.ih;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.Data;
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
    Objects.requireNonNull(ihWsUrl);

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

  public List<IHInstitution> getInstitutions() {
    return syncCall(api.listInstitutions()).getData();
  }

  public List<IHStaff> getStaffByInstitution(String institutionCode) {
    return syncCall(api.listStaff(institutionCode)).getData();
  }

  public List<String> getCountries() {
    return syncCall(api.listCountries()).getData();
  }

  private interface API {
    @GET("institutions")
    Call<InstitutionWrapper> listInstitutions();

    @GET("staff/search")
    Call<StaffWrapper> listStaff(@Query("code") String institutionCode);

    @GET("countries")
    Call<CountryWrapper> listCountries();
  }

  @Data
  private static class InstitutionWrapper {
    private IHMetadata meta;
    private List<IHInstitution> data;
  }

  @Data
  private static class StaffWrapper {
    private IHMetadata meta;
    private List<IHStaff> data = new ArrayList<>();
  }

  @Data
  private static class CountryWrapper {
    private IHMetadata meta;
    private List<String> data = new ArrayList<>();
  }
}
