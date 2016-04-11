package org.gbif.registry.metrics;

import java.util.UUID;

/**
 * Access to Occurrence metrics.
 */
public interface OccurrenceMetricsClient {

  /**
   * Get the number of record for a datasetKey.
   *
   * @param datasetKey
   * @return
   */
  Long getCountForDataset(UUID datasetKey);


}
