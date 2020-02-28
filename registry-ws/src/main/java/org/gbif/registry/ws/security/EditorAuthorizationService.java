/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.ws.security;

import java.util.UUID;

public interface EditorAuthorizationService {

  /**
   * Checks whether a given user is allowed to modify machine tags belonging to the given namespace.
   *
   * @param name name from the security context
   * @param ns the namespace in question
   * @return true if the user is allowed to modify the namespace
   */
  boolean allowedToModifyNamespace(String name, String ns);

  /** @return true if rights exist for this user to delete the tag */
  boolean allowedToDeleteMachineTag(String name, int machineTagKey);
  /**
   * Checks whether a given registry entity is explicitly part of the list of entities for an editor
   * user.
   *
   * @param name name from the security context
   * @param key the entity in question
   * @return true if the passed entity is allowed to be modified
   */
  boolean allowedToModifyEntity(String name, UUID key);

  boolean allowedToModifyDataset(String name, UUID datasetKey);

  boolean allowedToModifyOrganization(String name, UUID orgKey);

  boolean allowedToModifyInstallation(String name, UUID installationKey);
}
