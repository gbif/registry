package org.gbif.registry.service.collections.duplicates;

import org.gbif.api.model.collections.duplicates.DuplicatesResult;
import org.gbif.registry.persistence.mapper.collections.DuplicatesMapper;
import org.gbif.registry.persistence.mapper.collections.params.DuplicatesSearchParams;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InstitutionDuplicatesService extends BaseDuplicatesService {

  private final DuplicatesMapper duplicatesMapper;

  @Autowired
  public InstitutionDuplicatesService(DuplicatesMapper duplicatesMapper) {
    this.duplicatesMapper = duplicatesMapper;
  }

  @Override
  public DuplicatesResult findPossibleDuplicates(DuplicatesSearchParams params) {
    return processDBResults(
        duplicatesMapper::getInstitutionDuplicates,
        duplicatesMapper::getInstitutionsMetadata,
        params);
  }
}
