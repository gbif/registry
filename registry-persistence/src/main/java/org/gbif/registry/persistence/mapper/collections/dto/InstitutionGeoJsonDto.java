package org.gbif.registry.persistence.mapper.collections.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Data;

@Data
public class InstitutionGeoJsonDto {

  private UUID key;
  private String name;
  private BigDecimal latitude;
  private BigDecimal longitude;
}
