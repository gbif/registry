package org.gbif.registry.collections.sync.ih;

import lombok.Data;

/** Models an Index Herbariorum institution. */
@Data
public class IHInstitution implements IHEntity {

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
  public static class Address {
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
  public static class Contact {
    private String phone;
    private String email;
    private String webUrl;
  }

  @Data
  public static class Location {
    private Double lat;
    private Double lon;
  }
}
