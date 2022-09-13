package org.gbif.registry.persistence.mapper.collections.params;

import lombok.Data;

@Data
public class RangeParam {
  Integer lowerBound;
  Integer higherBound;
  Integer exactValue;
}
