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

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for grscicoll_vocab_concept table that stores vocabulary concept definitions
 * and their hierarchical relationships for GrSciColl entities.
 * Uses vocabulary keys for direct mapping with vocabulary system.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrSciCollVocabConceptDto {
  private Long conceptKey;
  private Long vocabularyKey;
  private String vocabularyName;
  private String name;
  private String path;
  private Long parentKey;
  private Long replacedByKey;
  private LocalDateTime deprecated;
  private String deprecatedBy;
}
