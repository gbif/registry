package org.gbif.registry.persistence.mapper.collections.dto;

import lombok.Data;

@Data
public class FacetDto {

  private String facet;
  private long count;
}
