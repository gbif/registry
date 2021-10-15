/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.metasync.api;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.metasync.MetasyncHistory;
import org.gbif.api.model.registry.metasync.MetasyncResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;

/** A simple holder object used to pass around the result of metadata synchronisation. */
@SuppressWarnings({"PublicField", "AssignmentToCollectionOrArrayFieldFromParameter"})
public class SyncResult {

  /**
   * Maps from the existing Dataset in the Registry to a new object that we just parsed from the
   * Endpoint.
   */
  public Map<Dataset, Dataset> existingDatasets;

  public List<Dataset> addedDatasets;
  public List<Dataset> deletedDatasets;
  public Installation installation;
  public MetadataException exception;

  public SyncResult(
      Map<Dataset, Dataset> existingDatasets,
      List<Dataset> addedDatasets,
      List<Dataset> deletedDatasets,
      Installation installation) {
    this.existingDatasets = existingDatasets;
    this.addedDatasets = addedDatasets;
    this.deletedDatasets = deletedDatasets;
    this.installation = installation;
  }

  public SyncResult(Installation installation, MetadataException exception) {
    this.installation = installation;
    this.exception = exception;
  }

  /** @return A metasync history summary of the result */
  public MetasyncHistory buildHistory() {
    MetasyncHistory history = new MetasyncHistory();
    history.setInstallationKey(installation.getKey());

    if (exception != null) {
      history.setResult(buildCode(exception.getErrorCode()));
      String message =
          String.format(
              "Synchronization failed with error [%s]. %d datasets were updated. %d datasets were added. %d datasets were deleted.",
              exception.getMessage(),
              sizeOf(existingDatasets),
              sizeOf(addedDatasets),
              sizeOf(deletedDatasets));
      history.setDetails(message);
    } else {
      history.setResult(MetasyncResult.OK);
      String message =
          String.format(
              "Synchronization succeeded. %d datasets were updated. %d datasets were added. %d datasets were deleted.",
              sizeOf(existingDatasets), sizeOf(addedDatasets), sizeOf(deletedDatasets));
      history.setDetails(message);
    }
    return history;
  }

  // NPE safe version
  private int sizeOf(Collection<?> collection) {
    return collection == null ? 0 : collection.size();
  }

  // NPE safe version
  private int sizeOf(Map<?, ?> collection) {
    return collection == null ? 0 : collection.size();
  }

  private MetasyncResult buildCode(ErrorCode code) {
    Preconditions.checkNotNull(code, "Cannot build a code from null input");
    switch (code) {
      case IO_EXCEPTION:
        return MetasyncResult.IO_EXCEPTION;
      case PROTOCOL_ERROR:
        return MetasyncResult.PROTOCOL_ERROR;
      case HTTP_ERROR:
        return MetasyncResult.HTTP_ERROR;
      case OTHER_ERROR:
        return MetasyncResult.OTHER_ERROR;
      default:
        return MetasyncResult.OTHER_ERROR;
    }
  }
}
