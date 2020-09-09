/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
