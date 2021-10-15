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
package org.gbif.registry.directory;

import org.gbif.api.model.registry.Node;

public interface Augmenter {

  /**
   * Adds all IMS infos found to an existing node instance which is required to have a country field
   * filled.
   *
   * @param node with country field filled
   * @return same node with IMS data added
   */
  Node augment(Node node);
}
