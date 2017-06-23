package org.gbif.registry.surety.persistence;

import org.gbif.registry.surety.model.ChallengeCode;

import java.util.UUID;

/**
 * Helper class to manage ChallengeCode with another table referencing it.
 */
public class ChallengeCodeManager<K> {

  private ChallengeCodeMapper challengeCodeMapper;
  private ChallengeCodeSupportMapper<K> challengeCodeSupportMapper;

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
   * Should be called inside a @Transactional method
   */
  public ChallengeCode create(K key) {
    ChallengeCode challengeCode = ChallengeCode.newRandom();
    challengeCodeMapper.createChallengeCode(challengeCode);
    challengeCodeSupportMapper.updateChallengeCodeKey(key, challengeCode.getKey());
    return challengeCode;
  }

  /**
   * Should be called inside a @Transactional method
   * @param key
   * @return
   */
  public boolean remove(K key) {
    Integer challengeCodeKey = challengeCodeSupportMapper.getChallengeCodeKey(key);
    if(challengeCodeKey == null) {
      return false;
    }
    //remove the challengeCode from the referencing table
    challengeCodeSupportMapper.updateChallengeCodeKey(key, null);
    challengeCodeMapper.deleteChallengeCode(challengeCodeKey);
    return true;
  }
}
