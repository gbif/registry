package org.gbif.registry.persistence.mapper.collections.dto;

import java.util.UUID;

public class DuplicateMetadataDto {

  private UUID key;
  private boolean active;
  private boolean isIh;
  private boolean isIdigbio;

  public UUID getKey() {
    return key;
  }

  public void setKey(UUID key) {
    this.key = key;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public boolean isIh() {
    return isIh;
  }

  public void setIh(boolean ih) {
    isIh = ih;
  }

  public boolean isIdigbio() {
    return isIdigbio;
  }

  public void setIdigbio(boolean idigbio) {
    isIdigbio = idigbio;
  }
}
