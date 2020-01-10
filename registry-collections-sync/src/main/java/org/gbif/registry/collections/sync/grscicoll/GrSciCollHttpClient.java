package org.gbif.registry.collections.sync.grscicoll;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.collections.sync.http.BasicAuthInterceptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.*;

import static org.gbif.registry.collections.sync.http.SyncCall.syncCall;

/** A lightweight GRSciColl client. */
public class GrSciCollHttpClient {

  private final API api;

  private GrSciCollHttpClient(String grSciCollWsUrl, String user, String password) {
    ObjectMapper mapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.addDeserializer(Country.class, new IsoDeserializer());
    mapper.registerModule(module);

    OkHttpClient okHttpClient =
        new OkHttpClient.Builder().addInterceptor(new BasicAuthInterceptor(user, password)).build();

    Retrofit retrofit =
        new Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(grSciCollWsUrl)
            .addConverterFactory(JacksonConverterFactory.create(mapper))
            .build();
    api = retrofit.create(API.class);
  }

  public static GrSciCollHttpClient create(String grSciCollWsUrl, String user, String password) {
    return new GrSciCollHttpClient(grSciCollWsUrl, user, password);
  }

  /** Returns all institutions in GrSciCol. */
  @SneakyThrows
  public List<Institution> getInstitutions() {
    List<Institution> result = new ArrayList<>();

    boolean endRecords = false;
    int offset = 0;
    while (!endRecords) {
      PagingResponse<Institution> response = syncCall(api.listInstitutions(1000, offset));
      endRecords = response.isEndOfRecords();
      offset += response.getLimit();
      result.addAll(response.getResults());
    }

    return result;
  }

  public void createInstitution(Institution institution) {
    syncCall(api.createInstitution(institution));
  }

  public void updateInstitution(Institution institution) {
    syncCall(api.updateInstitution(institution.getKey(), institution));
  }

  /** Returns all institutions in GrSciCol. */
  @SneakyThrows
  public List<Collection> getCollections() {
    List<Collection> result = new ArrayList<>();

    boolean endRecords = false;
    int offset = 0;
    while (!endRecords) {
      PagingResponse<Collection> response = syncCall(api.listCollections(1000, offset));
      endRecords = response.isEndOfRecords();
      offset += response.getLimit();
      result.addAll(response.getResults());
    }

    return result;
  }

  public void updateCollection(Collection collection) {
    syncCall(api.updateCollection(collection.getKey(), collection));
  }

  public void createPerson(Person person) {
    syncCall(api.createPerson(person));
  }

  public void updatePerson(Person person) {
    syncCall(api.updatePerson(person.getKey(), person));
  }

  public void deletePerson(Person person) {
    syncCall(api.deletePerson(person.getKey()));
  }

  private interface API {
    @GET("institution")
    Call<PagingResponse<Institution>> listInstitutions(
        @Query("limit") int limit, @Query("offset") int offset);

    @POST("institution")
    Call<UUID> createInstitution(@Body Institution institution);

    @PUT("institution/{key}")
    Call<Void> updateInstitution(@Path("key") UUID key, @Body Institution institution);

    @GET("collection")
    Call<PagingResponse<Collection>> listCollections(
        @Query("limit") int limit, @Query("offset") int offset);

    @PUT("collection/{key}")
    Call<Void> updateCollection(@Path("key") UUID key, @Body Collection collection);

    @POST("person")
    Call<UUID> createPerson(@Body Person person);

    @PUT("person/{key}")
    Call<Void> updatePerson(@Path("key") UUID key, @Body Person person);

    @DELETE("person/{key}")
    Call<Void> deletePerson(@Path("key") UUID key);
  }

  /** Adapter necessary for retrofit due to versioning. */
  private static class IsoDeserializer extends JsonDeserializer<Country> {
    @Override
    public Country deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
      try {
        if (jp != null && jp.getTextLength() > 0) {
          return Country.fromIsoCode(jp.getText());
        } else {
          return Country.UNKNOWN; // none provided
        }
      } catch (Exception e) {
        throw new IOException(
            "Unable to deserialize country from provided value (not an ISO 2 character?): "
                + jp.getText());
      }
    }
  }
}
