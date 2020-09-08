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
