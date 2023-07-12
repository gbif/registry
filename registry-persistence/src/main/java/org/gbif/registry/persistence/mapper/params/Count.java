package org.gbif.registry.persistence.mapper.params;

import java.util.UUID;

import lombok.Data;

/** Used for GRSciColl counts such as occurrence counts. */
@Data
public class Count {

  private UUID key;
  private long occurrenceCount;
  private long typeSpecimenCount;
}
