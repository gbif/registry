package org.gbif.registry.service;

import org.gbif.api.model.common.DOI;
import org.gbif.registry.persistence.mapper.OccurrenceDownloadMapper;
import org.springframework.stereotype.Service;

@Service
public class RegistryOccurrenceDownloadServiceImpl implements RegistryOccurrenceDownloadService {

  private final OccurrenceDownloadMapper occurrenceDownloadMapper;

  public RegistryOccurrenceDownloadServiceImpl(OccurrenceDownloadMapper occurrenceDownloadMapper) {
    this.occurrenceDownloadMapper = occurrenceDownloadMapper;
  }

  @Override
  public boolean checkOccurrenceDownloadExists(DOI doi) {
    return occurrenceDownloadMapper.getByDOI(doi) != null;
  }
}
