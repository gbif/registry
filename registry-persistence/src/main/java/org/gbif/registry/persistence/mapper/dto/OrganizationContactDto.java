package org.gbif.registry.persistence.mapper.dto;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.vocabulary.Country;

import java.util.UUID;

import lombok.Data;

@Data
public class OrganizationContactDto {

  private Contact contact;
  private UUID organizationKey;
  private Country organizationCountry;
  private String organizationTitle;
}
