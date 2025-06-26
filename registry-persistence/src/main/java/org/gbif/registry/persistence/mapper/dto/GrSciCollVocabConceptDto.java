package org.gbif.registry.persistence.mapper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
