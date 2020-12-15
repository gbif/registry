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
package org.gbif.registry.persistence.mapper.collections;

import java.util.UUID;

import org.apache.ibatis.annotations.Param;

public interface MergeableMapper {

  void replace(
      @Param("targetEntityKey") UUID targetEntityKey, @Param("replacementKey") UUID replacementKey);

  void moveIdentifier(
      @Param("sourceEntityKey") UUID sourceEntityKey,
      @Param("targetEntityKey") UUID targetEntityKey,
      @Param("identifierKey") int identifierKey);

  void moveMachineTag(
      @Param("sourceEntityKey") UUID sourceEntityKey,
      @Param("targetEntityKey") UUID targetEntityKey,
      @Param("machineTagKey") int machineTagKey);

  void moveOccurrenceMapping(
      @Param("sourceEntityKey") UUID sourceEntityKey,
      @Param("targetEntityKey") UUID targetEntityKey,
      @Param("occurrenceMappingKey") int occurrenceMappingKey);
}
