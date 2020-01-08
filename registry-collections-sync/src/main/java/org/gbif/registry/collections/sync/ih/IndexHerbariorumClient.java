package org.gbif.registry.collections.sync.ih;

import java.util.List;

import lombok.Data;
import lombok.SneakyThrows;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;

import static org.gbif.registry.collections.sync.http.SyncCall.syncCall;

/** Lightweight IndexHerbariorum client. */
public class IndexHerbariorumClient {

  private final API api;

  private IndexHerbariorumClient(String ihWsUrl) {
    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(ihWsUrl)
            .addConverterFactory(JacksonConverterFactory.create())
            .build();
    api = retrofit.create(API.class);
  }

  public static IndexHerbariorumClient create(String ihWsUrl) {
    return new IndexHerbariorumClient(ihWsUrl);
  }

  @SneakyThrows
  public List<IhInstitution> getInstitutions() {
    return syncCall(api.listInstitutions()).getData();
  }

  private interface API {
    @GET("institutions")
    Call<InstitutionWrapper> listInstitutions();
  }

  @Data
  private static class InstitutionWrapper {
    private Meta meta;
    private List<IhInstitution> data;

    @Data
    private static class Meta {
      private int hits;
      private int code;
    }
  }
}
