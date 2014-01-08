package org.gbif.registry.metasync.resulthandler;

import org.gbif.registry.metasync.api.SyncResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple utility class that prints some information about each synchronisation result. Does not generate aggregate
 * statistics at the moment.
 */
public final class DebugHandler {

  private static final Logger LOG = LoggerFactory.getLogger(DebugHandler.class);

  public static void processResults(Iterable<SyncResult> results) {
    for (SyncResult result : results) {
      processResult(result);
    }
  }

  public static void processResult(SyncResult result) {
    if (result == null) {
      LOG.warn("Null SyncResult!");
    } else if (result.exception == null) {
      LOG.info("Installation [{}] synced successfully. [{}] added, [{}] deleted, [{}] updated",
               result.installation.getKey(),
               result.addedDatasets.size(),
               result.deletedDatasets.size(),
               result.existingDatasets.size());
    } else {
      LOG.info("Installation [{}] failed sync. Reason: [{}]",
               result.installation.getKey(),
               result.exception.getErrorCode());
      if (result.exception.getMessage() != null) {
        LOG.info("Message: [{}]", result.exception.getMessage());
      }
      if (result.exception.getCause() != null) {
        LOG.info("Cause: [{}], [{}]",
                 result.exception.getCause().getClass().toString(),
                 result.exception.getCause().getMessage());
      }
    }
  }

  private DebugHandler() {
    throw new UnsupportedOperationException("Can't initialize class");
  }

}
