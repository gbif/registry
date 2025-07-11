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
package org.gbif.registry.persistence.mapper.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for representing entity-concept relationships during vocabulary updates.
 * Used to preserve and restore links between institutions/collections and concepts.
 * Uses vocabulary concept keys for direct mapping.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrsciCollConceptLinkDto {
  private UUID entityKey;
  private Long conceptKey;
  private String conceptName;
}
