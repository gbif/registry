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
