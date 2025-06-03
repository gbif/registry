package org.gbif.registry.persistence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for grscicoll_vocab_facet table that stores vocabulary facet definitions 
 * and their hierarchical relationships for GrSciColl entities.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrSciCollVocabFacetDto {
  private Integer id; // Corresponds to SERIAL in PostgreSQL
  private String vocabularyName;
  private String name; // Display name for the facet term
  private String path; // LTREE path as a String
} 