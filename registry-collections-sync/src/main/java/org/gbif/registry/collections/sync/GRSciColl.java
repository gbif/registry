package org.gbif.registry.collections.sync;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.vocabulary.Country;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A lightweight GRSciColl client.
 */
public class GRSciColl {
  private static API api = newClient();

  /**
   * Returns all institutions in GrSciCol.
   */
  static List<Institution> institutions() throws IOException {
    List<Institution> result = new ArrayList<>();
    boolean endRecords = false;
    int offset = 0;
    while (!endRecords) {
      PagingResponse<Institution> response = api.listInstitutions(1000, offset).execute().body();
      endRecords = response.isEndOfRecords();
      offset += response.getLimit();
      result.addAll(response.getResults());
    }
    return result;
  }

  /**
   * Returns all institutions in GrSciCol.
   */
  static List<Collection> collections() throws IOException {
    List<Collection> result = new ArrayList<>();
    boolean endRecords = false;
    int offset = 0;
    while (!endRecords) {
      PagingResponse<Collection> response = api.listCollections(1000, offset).execute().body();
      endRecords = response.isEndOfRecords();
      offset += response.getLimit();
      result.addAll(response.getResults());
    }
    return result;
  }

  private static API newClient() {
    ObjectMapper mapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.addDeserializer(Country.class, new IsoDeserializer());
    mapper.registerModule(module);

    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl("http://api.gbif.org/v1/grscicoll/") // TODO parameterize
      .addConverterFactory(JacksonConverterFactory.create(mapper))
      .build();
    return retrofit.create(API.class);
  }

  private interface API {
    @GET("institution")
    Call<PagingResponse<Institution>> listInstitutions(@Query("limit") int limit, @Query("offset") int offset);

    @GET("collection")
    Call<PagingResponse<Collection>> listCollections(@Query("limit") int limit, @Query("offset") int offset);
  }

  /**
   * Adapter necessary for retrofit due to versioning.
   */
  private static class IsoDeserializer extends JsonDeserializer<Country> {
    @Override
    public Country deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
      try {
        if (jp != null && jp.getTextLength() > 0) {
          return Country.fromIsoCode(jp.getText());
        } else {
          return Country.UNKNOWN; // none provided
        }
      } catch (Exception e) {
        throw new IOException("Unable to deserialize country from provided value (not an ISO 2 character?): "
          + jp.getText());
      }
    }
  }


}
