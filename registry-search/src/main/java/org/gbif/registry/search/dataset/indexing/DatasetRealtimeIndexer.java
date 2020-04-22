package org.gbif.registry.search.dataset.indexing;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;

public interface DatasetRealtimeIndexer {

  void index(Dataset dataset);

  void index(Iterable<Dataset> datasets);

  void index(Organization organization);

  void index(Installation installation);

  void delete(Dataset dataset);

  int getPendingUpdates();
}
