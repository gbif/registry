package org.gbif.registry.ws.model;

import org.gbif.api.vocabulary.InstallationType;

import javax.annotation.Nullable;

public class InstallationRequestSearchParams extends RequestSearchParams {

  private InstallationType type; // installationType

  @Nullable
  public InstallationType getType() {
    return type;
  }

  public void setType(@Nullable InstallationType type) {
    this.type = type;
  }
}
