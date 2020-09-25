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
package org.gbif.registry.surety;

import org.gbif.api.model.ChallengeCode;

import java.util.UUID;

/**
 * Helper class to manage challengeCode references it.
 *
 * @param <K> type of the key used by the entity who is pointing to the challengeCode.
 */
public interface ChallengeCodeManager<K> {

  /**
   * Check if the provided challengeCode is valid for a specific entity key and data.
   */
  boolean isValidChallengeCode(K key, UUID challengeCode, String data);

  /**
   * Check if a given key is associated with a challengeCode.
   */
  boolean hasChallengeCode(K key);

  /**
   * Creates a new challengeCode and updates the link between the entity and the challengeCode.
   * Should be called inside a @Transactional method
   */
  ChallengeCode create(K key);

  /**
   * Creates a new challengeCode with additional data
   * and updates the link between the entity and the challengeCode.
   * Should be called inside a @Transactional method
   */
  ChallengeCode create(K key, String data);

  /**
   * Removes a challengeCode and removes the link between the entity and the challengeCode. Should
   * be called inside a @Transactional method
   */
  void remove(K key);
}
