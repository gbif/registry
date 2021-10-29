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
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;

import java.util.List;

/** Interface to be implemented to support a new protocol for Metadata requests. */
public interface MetadataProtocolHandler {

  /**
   * Determines if this handler does support the Installation in question. If it returns {@code
   * true} then {@link #syncInstallation(Installation, List)} installation will be called with the
   * {@link Installation}.
   */
  boolean canHandle(Installation installation);

  /**
   * Does metadata synchronisation against the passed in Installation.
   *
   * @param installation to synchronise against
   * @param datasets all the datasets currently hosted by this Installation
   */
  // TODO: Document if return can be null
  SyncResult syncInstallation(Installation installation, List<Dataset> datasets)
      throws MetadataException;

  /** Retrieves the dataset count for a dataset at an endpoint. */
  Long getDatasetCount(Dataset dataset, Endpoint endpoint) throws MetadataException;
}
