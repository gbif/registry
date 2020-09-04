package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.service.InvalidMetadataException;

import java.net.URI;

/**
 * Service manages DOIs directly in the DB.
 * See also {@link DoiMessageManagingService}.
 */
public interface DoiDirectManagingService {

  /**
   * Updates the doi table to status FAILED and uses the error message & stacktrace as the xml for
   * manual debugging / cleanup.
   */
  void failed(DOI doi, InvalidMetadataException e);

  /**
   * Updates the doi table with metadata and target.
   */
  void update(DOI doi, DataCiteMetadata metadata, URI target);
}
