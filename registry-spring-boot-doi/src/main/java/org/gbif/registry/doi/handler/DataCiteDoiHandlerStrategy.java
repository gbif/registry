package org.gbif.registry.doi.handler;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.Dataset;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.RelationType;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Business logic for DOI handling with DataCite.
 */
public interface DataCiteDoiHandlerStrategy {

  /**
   * Tells is a DOI is using a predefined prefix.
   */
  boolean isUsingMyPrefix(DOI doi);

  /**
   * Build the DataCiteMetadata for a Download.
   */
  DataCiteMetadata buildMetadata(Download download, GbifUser user);

  /**
   * Build the DataCiteMetadata for a Dataset.
   */
  DataCiteMetadata buildMetadata(Dataset dataset);

  /**
   * Build the DataCiteMetadata for a Dataset that includes a relation to another DOI.
   */
  DataCiteMetadata buildMetadata(Dataset dataset, @Nullable DOI related, @Nullable RelationType relationType);

  /**
   * Called when some data related to the Dataset changed. The implementation decides the action to take with the DOI
   * service.
   *
   * @param previousDoi DOI of this dataset prior to the update.
   */
  void datasetChanged(Dataset dataset, @Nullable final DOI previousDoi);

  /**
   * Called when some data in the Download changed. The implementation decides the action to take with the DOI
   * service.
   *
   * @param previousDownload download object as it appears before the change or null if the change is triggered
   *                         by something else
   */
  void downloadChanged(Download download, Download previousDownload, GbifUser user);

  /**
   * Directly schedule the registration of a Dataset DOI.
   */
  void scheduleDatasetRegistration(DOI doi, DataCiteMetadata metadata, UUID datasetKey);
}
