package org.gbif.registry.domain.ws;

import javax.annotation.Nullable;

public class OrganizationRequestSearchParams extends RequestSearchParams {

  private Boolean isEndorsed;

  @Nullable
  public Boolean getIsEndorsed() {
    return isEndorsed;
  }

  public void setIsEndorsed(@Nullable Boolean isEndorsed) {
    this.isEndorsed = isEndorsed;
  }
}
