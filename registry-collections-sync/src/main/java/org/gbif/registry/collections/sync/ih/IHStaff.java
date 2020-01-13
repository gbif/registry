package org.gbif.registry.collections.sync.ih;

import lombok.Builder;
import lombok.Data;

/** Models an Index Herbariorum staff. */
@Data
@Builder
public class IHStaff {

  private String irn;
  private String code;
  private String lastName;
  private String middleName;
  private String firstName;
  private String birthDate;
  private String correspondent;
  private String position;
  private String specialities;
  private Address address;
  private Contact contact;
  private String dateModified;

  @Data
  @Builder
  public static class Address {
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
  }

  @Data
  @Builder
  public static class Contact {
    private String phone;
    private String email;
    private String fax;
  }
}
