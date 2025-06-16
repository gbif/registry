package org.gbif.registry.search.dataset.indexing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Custom VocabularyConcept class for dataset category processing.
 * Contains concept name and its lineage (parent categories).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyConcept {
  private String concept;
  private List<String> lineage;
} 