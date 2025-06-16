package org.gbif.registry.search.dataset.indexing;

import org.gbif.vocabulary.api.ConceptView;
import org.gbif.vocabulary.client.ConceptClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Factory class for creating VocabularyConcept instances from ConceptView.
 */
public class VocabularyConceptFactory {

  private static final Logger LOG = LoggerFactory.getLogger(VocabularyConceptFactory.class);

  /**
   * Creates a VocabularyConcept from a ConceptView, including its lineage.
   * 
   * @param conceptView The ConceptView to convert (should already include parent info)
   * @return Optional VocabularyConcept with lineage
   */
  public static Optional<VocabularyConcept> createConcept(ConceptView conceptView) {
    
    if (conceptView == null || conceptView.getConcept() == null) {
      return Optional.empty();
    }

    String conceptName = conceptView.getConcept().getName();
    List<String> lineage = buildLineage(conceptView);

    return Optional.of(new VocabularyConcept(conceptName, lineage));
  }

  /**
   * Creates a VocabularyConcept from a concept name, including its lineage.
   * 
   * @param conceptName The concept name to fetch and convert
   * @param conceptClient The ConceptClient to fetch parent concepts
   * @param vocabularyName The vocabulary name
   * @return Optional VocabularyConcept with lineage
   */
  public static Optional<VocabularyConcept> createConceptFromName(
      String conceptName, 
      ConceptClient conceptClient, 
      String vocabularyName) {
    
    if (conceptName == null || conceptName.trim().isEmpty()) {
      return Optional.empty();
    }

    try {
      ConceptView conceptView = conceptClient.getFromLatestRelease(vocabularyName, conceptName, true, false);
      return createConcept(conceptView);
    } catch (Exception e) {
      LOG.error("Error fetching concept {} from vocabulary {}: {}", conceptName, vocabularyName, e.getMessage(), e);
      // Return concept with just itself as lineage
      return Optional.of(new VocabularyConcept(conceptName, List.of(conceptName)));
    }
  }

  /**
   * Builds the lineage (parent categories + self) for a concept.
   * 
   * @param conceptView The concept to build lineage for (should already include parent info)
   * @return List of category names (from root to self)
   */
  private static List<String> buildLineage(ConceptView conceptView) {
    
    List<String> lineage = new ArrayList<>();
    
    if (conceptView != null && conceptView.getConcept() != null) {
      // Add parents first (from root to immediate parent)
      if (conceptView.getParents() != null) {
        lineage.addAll(conceptView.getParents());
      }
      
      // Add self at the end
      lineage.add(conceptView.getConcept().getName());
    }
    
    return lineage;
  }
} 