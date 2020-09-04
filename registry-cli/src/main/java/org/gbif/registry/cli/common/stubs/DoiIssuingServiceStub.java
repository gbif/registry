package org.gbif.registry.cli.common.stubs;

import org.gbif.api.model.common.DOI;
import org.gbif.registry.doi.DoiIssuingService;

public class DoiIssuingServiceStub implements DoiIssuingService {
  @Override
  public DOI newDatasetDOI() {
    return null;
  }

  @Override
  public DOI newDerivedDatasetDOI() {
    return null;
  }

  @Override
  public DOI newDownloadDOI() {
    return null;
  }

  @Override
  public DOI newDataPackageDOI() {
    return null;
  }

  @Override
  public boolean isGbif(DOI doi) {
    return false;
  }
}
