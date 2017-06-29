package org.gbif.registry.surety.persistence;

import org.gbif.registry.surety.model.ChallengeCode;

import java.util.UUID;

/**
 * Helper class to manage challengeCode references it.
 * @param <K> type of the key used by the entity who is pointing to the challengeCode.
 */
public class ChallengeCodeManager<K> {

  private final ChallengeCodeMapper challengeCodeMapper;
  private final ChallengeCodeSupportMapper<K> challengeCodeSupportMapper;

  /**
   *
   * @param challengeCodeMapper
   * @param challengeCodeSupportMapper
   */
  public ChallengeCodeManager(ChallengeCodeMapper challengeCodeMapper, ChallengeCodeSupportMapper<K> challengeCodeSupportMapper) {
    this.challengeCodeMapper = challengeCodeMapper;
    this.challengeCodeSupportMapper = challengeCodeSupportMapper;
  }

  /**
   * Check if the provided challengeCode is valid for a specific entity key.
   * @param key
   * @param challengeCode
   * @return
   */
  public boolean isValidChallengeCode(K key, UUID challengeCode) {
    if (key == null || challengeCode == null) {
      return false;
    }
    Integer ccKey = challengeCodeSupportMapper.getChallengeCodeKey(key);
    return ccKey != null && challengeCode.equals(challengeCodeMapper.getChallengeCode(ccKey));
  }

  /**
   * Check if a given key is associated with a challengeCode.
   * @param key
   * @return
   */
  public boolean hasChallengeCode(K key) {
    return challengeCodeSupportMapper.getChallengeCodeKey(key) != null;
  }

  /**
   * Creates a new challengeCode and updates the link between the entity and the challengeCode.
   * Should be called inside a @Transactional method
   */
  public ChallengeCode create(K key) {
    ChallengeCode challengeCode = ChallengeCode.newRandom();
    challengeCodeMapper.createChallengeCode(challengeCode);
    challengeCodeSupportMapper.updateChallengeCodeKey(key, challengeCode.getKey());
    return challengeCode;
  }

  /**
   * Removes a challengeCode and removes the link between the entity and the challengeCode.
   * Should be called inside a @Transactional method
   * @param key
   * @return the challengeCode was removed successfully.
   */
  public boolean remove(K key) {
    Integer challengeCodeKey = challengeCodeSupportMapper.getChallengeCodeKey(key);
    if(challengeCodeKey == null) {
      return false;
    }
    //remove the challengeCode from the referencing table first
    challengeCodeSupportMapper.updateChallengeCodeKey(key, null);
    challengeCodeMapper.deleteChallengeCode(challengeCodeKey);
    return true;
  }
}
