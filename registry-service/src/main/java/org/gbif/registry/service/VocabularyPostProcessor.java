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