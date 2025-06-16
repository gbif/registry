package org.gbif.registry.service;

/**
 * Interface for processing vocabulary releases with vocabulary-specific logic.
 * Each vocabulary can have its own post-processing steps after the concepts are updated.
 */
public interface VocabularyPostProcessor {

  /**
   * Check if this processor can handle the given vocabulary.
   * 
   * @param vocabularyName the vocabulary name
   * @return true if this processor should handle this vocabulary
   */
  boolean canHandle(String vocabularyName);

  /**
   * Process the vocabulary release with vocabulary-specific logic.
   * 
   * @param vocabularyName the vocabulary name that was released
   * @return the number of items processed
   */
  int process(String vocabularyName);
} 