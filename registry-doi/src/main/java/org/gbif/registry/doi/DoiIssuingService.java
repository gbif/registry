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
package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;

/**
 * Service that guarantees to issue unique new DOIs.
 */
public interface DoiIssuingService {

  /**
   * Generates a new unique GBIF dataset DOI. The new DOI is unknown to DataCite still and only
   * lives in the GBIF registry which guarantees it to be unique.
   *
   * @return the new DOI
   */
  DOI newDatasetDOI();

  /**
   * Generates a new unique GBIF derived dataset DOI. The new DOI is unknown to DataCite still and only
   * lives in the GBIF registry which guarantees it to be unique.
   *
   * @return the new DOI
   */
  DOI newDerivedDatasetDOI();

  /**
   * Generates a new unique GBIF download DOI. The new DOI is unknown to DataCite still and only
   * lives in the GBIF registry which guarantees it to be unique.
   *
   * @return the new DOI
   */
  DOI newDownloadDOI();

  /**
   * Generates a new unique GBIF data package DOI. The new DOI is unknown to DataCite still and only
   * lives in the GBIF registry which guarantees it to be unique.
   *
   * @return the new DOI
   */
  DOI newDataPackageDOI();

  /**
   * Tests a DOI to see if it was issued by GBIF.
   *
   * @return true if DOI was issued by GBIF
   */
  boolean isGbif(DOI doi);
}
