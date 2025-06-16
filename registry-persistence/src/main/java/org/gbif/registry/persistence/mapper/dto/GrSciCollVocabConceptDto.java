package org.gbif.registry.persistence.mapper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for grscicoll_vocab_concept table that stores vocabulary concept definitions
 * and their hierarchical relationships for GrSciColl entities.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrSciCollVocabConceptDto {
  private Integer id; // Corresponds to SERIAL in PostgreSQL
  private String vocabularyName;
  private String name; // Display name for the concept term
  private String path; // LTREE path as a String
}
