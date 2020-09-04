package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Dataset;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;

import javax.annotation.Nullable;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Business logic for Dataset DOI handling with DataCite.
 */
public interface DatasetDoiDataCiteHandlingService {

  /**
   * Called when some data related to the Dataset changed. The implementation decides the action to
   * take with the DOI service.
   *
   * @param previousDoi DOI of this dataset prior to the update.
   */
  void datasetChanged(Dataset dataset, @Nullable final DOI previousDoi);

  /**
   * Directly schedule the registration of a Dataset DOI.
   */
  void scheduleDatasetRegistration(DOI doi, DataCiteMetadata metadata, UUID datasetKey);

  /**
   * Directly schedule the registration of a Derived Dataset DOI.
   */
  void scheduleDerivedDatasetRegistration(DOI doi, DataCiteMetadata metadata, URI target, LocalDate registrationDate);
}
