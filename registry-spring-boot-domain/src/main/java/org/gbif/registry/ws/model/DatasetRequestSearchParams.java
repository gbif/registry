package org.gbif.registry.ws.model;

import org.gbif.api.vocabulary.DatasetType;

import javax.annotation.Nullable;

public class DatasetRequestSearchParams extends RequestSearchParams {

  private DatasetType type; // datasetType

  @Nullable
  public DatasetType getType() {
    return type;
  }

  public void setType(@Nullable DatasetType type) {
    this.type = type;
  }
}
