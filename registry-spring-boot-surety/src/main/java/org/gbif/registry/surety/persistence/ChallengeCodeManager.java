package org.gbif.registry.surety.persistence;

import org.gbif.api.model.ChallengeCode;

import java.util.UUID;

/**
 * Helper class to manage challengeCode references it.
 *
 * @param <K> type of the key used by the entity who is pointing to the challengeCode.
 */
public interface ChallengeCodeManager<K> {

  /**
   * Check if the provided challengeCode is valid for a specific entity key.
   */
  boolean isValidChallengeCode(K key, UUID challengeCode);

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
   * Removes a challengeCode and removes the link between the entity and the challengeCode.
   * Should be called inside a @Transactional method
   *
   * @return the challengeCode was removed successfully.
   */
  boolean remove(K key);
}
