package org.gbif.registry.persistence.mapper.collections.dto;

import java.util.UUID;

public class MasterSourceOrganizationDto {

  private UUID collectionKey;
  private UUID datasetKey;

  public UUID getCollectionKey() {
    return collectionKey;
  }

  public void setCollectionKey(UUID collectionKey) {
    this.collectionKey = collectionKey;
  }

  public UUID getDatasetKey() {
    return datasetKey;
  }

  public void setDatasetKey(UUID datasetKey) {
    this.datasetKey = datasetKey;
  }
}
