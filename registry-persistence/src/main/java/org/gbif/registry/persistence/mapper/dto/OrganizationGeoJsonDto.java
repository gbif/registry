package org.gbif.registry.persistence.mapper.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Data;

@Data
public class OrganizationGeoJsonDto {

  private UUID key;
  private String title;
  private Integer numPublishedDatasets;
  private BigDecimal latitude;
  private BigDecimal longitude;

}
