package org.gbif.registry.collections.sync;

import lombok.Data;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;
import java.io.IOException;
import java.util.List;

/**
 * Lightweight IndexHerbariorum client.
 */
class IndexHerbariorum {

  static List<IndexHerbariorum.Institution> institutions() throws IOException {
    return newClient().listInstitutions().execute().body().getData();
  }


  private static API newClient() {
    Retrofit retrofit = new Retrofit.Builder()
      //.baseUrl("http://sweetgum.nybg.org/science/api/v1/") // TODO: Parameterize
      .baseUrl("http://labs.gbif.org/") // go easy on them...
      .addConverterFactory(JacksonConverterFactory.create())
      .build();
    return retrofit.create(API.class);
  }

  private interface API {
    @GET("institutions")
    Call<InstitutionWrapper> listInstitutions();
  }

  @Data
  private static class InstitutionWrapper {
    private Meta meta;
    private List<Institution> data;

    @Data
    private static class Meta {
      private int hits;
      private int code;
    }
  }


  @Data
  static class Institution {
    private String irn;
    private String organization;
    private String code;
    private String division;
    private String department;
    private long specimenTotal;
    private Address address;
    private Contact contact;
    private Location location;
    private String dateModified;

    @Data
    static class Address {
      private String physicalStreet;
      private String physicalCity;
      private String physicalState;
      private String physicalZipCode;
      private String physicalCountry;
      private String postalStreet;
      private String postalCity;
      private String postalState;
      private String postalZipCode;
      private String postalCountry;
    }

    @Data
    static class Contact {
      private String phone;
      private String email;
      private String webUrl;
    }

    @Data
    static class Location {
      private Double lat;
      private Double lon;
    }
  }
}
