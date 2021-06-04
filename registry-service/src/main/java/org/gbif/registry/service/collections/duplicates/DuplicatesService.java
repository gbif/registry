package org.gbif.registry.service.collections.duplicates;

import org.gbif.api.model.collections.duplicates.DuplicatesResult;
import org.gbif.registry.persistence.mapper.collections.params.DuplicatesSearchParams;

public interface DuplicatesService {

  DuplicatesResult findPossibleDuplicates(DuplicatesSearchParams params);
}
