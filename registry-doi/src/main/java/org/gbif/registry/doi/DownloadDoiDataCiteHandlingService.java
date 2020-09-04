package org.gbif.registry.doi;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;

/** Business logic for Download DOI handling with DataCite. */
public interface DownloadDoiDataCiteHandlingService {

  /**
   * Called when some data in the Download changed. The implementation decides the action to take
   * with the DOI service.
   *
   * @param previousDownload download object as it appears before the change or null if the change
   *     is triggered by something else
   */
  void downloadChanged(Download download, Download previousDownload, GbifUser user);
}
