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
package org.gbif.registry.persistence.service;

public enum MapperType {
  ORGANIZATION("organizationMapper"),
  CONTACT("contactMapper"),
  ENDPOINT("endpointMapper"),
  MACHINE_TAG("machineTagMapper"),
  TAG("tagMapper"),
  IDENTIFIER("identifierMapper"),
  COMMENT("commentMapper"),
  DATASET("datasetMapper"),
  INSTALLATION("installationMapper"),
  NODE("nodeMapper"),
  NETWORK("networkMapper"),
  METADATA("metadataMapper"),
  DATASET_PROCESS_STATUS("datasetProcessStatusMapper"),
  META_SYNC_HISTORY_MAPPER("metaSyncHistoryMapper");

  private final String name;

  MapperType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
