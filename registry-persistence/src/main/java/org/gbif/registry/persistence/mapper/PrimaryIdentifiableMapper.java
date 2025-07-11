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
package org.gbif.registry.persistence.mapper;

import java.util.UUID;

import org.apache.ibatis.annotations.Param;

public interface PrimaryIdentifiableMapper extends IdentifiableMapper {

  /**
   * Adds a collection identifier to the specified entity.
   *
   * @param entityKey    the UUID of the target entity to which the identifier will be added
   * @param identifierKey the key of the identifier to be added
   * @param isPrimary     true if the identifier should be marked as primary; false otherwise
   * @return the identifier key of the added identifier
   */
  int addCollectionIdentifier(
    @Param("targetEntityKey") UUID entityKey,
    @Param("identifierKey") int identifierKey,
    @Param("isPrimary") boolean isPrimary);

  /**
   * Updates the identifiers of the given entity, making any existing primary identifier non-primary.
   *
   * @param entityKey the UUID of the target entity whose primary identifier will be updated
   */
  void setAllIdentifiersToNonPrimary(
    @Param("targetEntityKey") UUID entityKey);

  /**
   * Updates the current primary identifier of the specified entity, making it non-primary.
   *
   * @param entityKey     the UUID of the target entity
   * @param identifierKey the key of the identifier to update
   * @param primary       true if the identifier is the new primary identifier; false otherwise
   */
  void updateIdentifier(
    @Param("targetEntityKey") UUID entityKey,
    @Param("identifierKey") int identifierKey,
    @Param("primary") boolean primary);

  /**
   * Checks if the specified identifier is associated with the given entity.
   *
   * @param targetEntityKey the UUID of the target entity
   * @param identifierKey   the key of the identifier to check
   * @return true if the identifier is associated with the entity; false otherwise
   */
  Boolean areRelated(
    @Param("targetEntityKey") UUID targetEntityKey,
    @Param("identifierKey") int identifierKey);
}
