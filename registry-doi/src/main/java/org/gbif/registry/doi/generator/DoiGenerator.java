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
package org.gbif.registry.doi.generator;

import org.gbif.api.model.common.DOI;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.InvalidMetadataException;

import java.util.UUID;

/**
 * Service that guarantees to issue unique new DOIs and deals with scheduling DOI metadata updates
 * and registration.
 */
public interface DoiGenerator {

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

  /**
   * Updates the doi table to status FAILED and uses the error message & stacktrace as the xml for
   * manual debugging / cleanup.
   */
  void failed(DOI doi, InvalidMetadataException e);

  /**
   * Schedules a DOI metadata update with DataCite and registers the DOI if needed. For subsequent
   * calls with the same DOI only the metadata in DataCite will be updated. If it is called for the
   * very first time the DOI will also be properly registered with DataCite.
   *
   * @param doi the GBIF DOI to registerDataset
   * @param metadata the metadata to post to datacite. Mandatory fields are validated immediately
   * @param datasetKey the dataset key to derive the target URL from
   * @throws InvalidMetadataException in case the metadata is missing mandatory fields or the DOI is
   *     not a GBIF one
   */
  void registerDataset(DOI doi, DataCiteMetadata metadata, UUID datasetKey)
      throws InvalidMetadataException;

  /**
   * Schedules a DOI metadata update with DataCite and registers the DOI if needed. For subsequent
   * calls with the same DOI only the metadata in DataCite will be updated. If it is called for the
   * very first time the DOI will also be properly registered with DataCite.
   *
   * @param doi the GBIF DOI to registerDataset
   * @param metadata the metadata to post to datacite. Mandatory fields are validated immediately
   * @param downloadKey the download key to derive the target URL from
   * @throws InvalidMetadataException in case the metadata is missing mandatory fields or the DOI is
   *     not a GBIF one
   */
  void registerDownload(DOI doi, DataCiteMetadata metadata, String downloadKey)
      throws InvalidMetadataException;

  /**
   * Schedules a DOI metadata update with DataCite and registers the DOI if needed. For subsequent
   * calls with the same DOI only the metadata in DataCite will be updated. If it is called for the
   * very first time the DOI will also be properly registered with DataCite.
   *
   * @param doi the GBIF DOI to registerDataset
   * @param metadata the metadata to post to datacite. Mandatory fields are validated immediately
   * @throws InvalidMetadataException in case the metadata is missing mandatory fields or the DOI is
   *     not a GBIF one
   */
  void registerDataPackage(DOI doi, DataCiteMetadata metadata) throws InvalidMetadataException;

  /**
   * Deletes a GBIF DOI. If the DOI is registered in DataCite it's deleted; otherwise, it's only
   * removed in the GBIF Registry.
   *
   * @param doi the GBIF DOI to delete/unregister
   */
  void delete(DOI doi);
}
