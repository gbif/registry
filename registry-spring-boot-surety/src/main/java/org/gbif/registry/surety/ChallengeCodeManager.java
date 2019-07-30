package org.gbif.registry.surety;

import org.gbif.api.model.ChallengeCode;
import org.gbif.registry.persistence.ChallengeCodeSupportMapper;
import org.gbif.registry.persistence.mapper.ChallengeCodeMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

// TODO: 2019-06-26 check all methods
/**
 * Helper class to manage challengeCode references it.
 * @param <K> type of the key used by the entity who is pointing to the challengeCode.
 */
@Service
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
    return Optional.ofNullable(challengeCodeSupportMapper.getChallengeCodeKey(key)).isPresent();
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
    return Optional.ofNullable(challengeCodeSupportMapper.getChallengeCodeKey(key))
        .map(challengeCodeKey -> {
          //remove the challengeCode from the referencing table first
          challengeCodeSupportMapper.updateChallengeCodeKey(key, null);
          challengeCodeMapper.deleteChallengeCode(challengeCodeKey);
          return true;
        }).orElse(Boolean.FALSE);
  }
}
