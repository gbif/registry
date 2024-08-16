package org.gbif.registry.persistence.mapper.collections.dto;

import lombok.Data;

@Data
public class VerbatimDto {

  private long key;
  private String fieldName;
  private String fieldValue;
}
