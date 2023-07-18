package org.gbif.registry.persistence.mapper.params;

import java.util.UUID;

import lombok.Data;

@Data
public class OrganizationOccurrenceDownloadDto {

  private String downloadKey;
  private UUID datasetKey;
  private UUID organizationKey;
  private String organizationTitle;
  private long numberRecords;
  private String publishingCountryCode;
}
