package org.gbif.registry.service;

import org.gbif.api.model.common.DOI;

public interface RegistryOccurrenceDownloadService {

  boolean checkOccurrenceDownloadExists(DOI doi);
}
