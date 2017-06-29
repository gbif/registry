package org.gbif.identity.mybatis;

import org.gbif.registry.surety.persistence.ChallengeCodeMapper;
import org.gbif.registry.surety.persistence.ChallengeCodeSupportMapper;

import java.util.UUID;

import org.mybatis.guice.transactional.Transactional;

/**
 *
 */
public class IdentitySuretyTestHelper {

  private ChallengeCodeMapper challengeCodeMapper;
  private ChallengeCodeSupportMapper<Integer> challengeCodeSupportMapper;

  public IdentitySuretyTestHelper(ChallengeCodeMapper challengeCodeMapper,
                           ChallengeCodeSupportMapper<Integer> challengeCodeSupportMapper) {
    this.challengeCodeMapper = challengeCodeMapper;
    this.challengeCodeSupportMapper = challengeCodeSupportMapper;
  }

  /**
   * Confirm an entity directly.
   * @param entityKey
   */
  @Transactional
  public void confirmEntity(Integer entityKey) {
    Integer challengeCodeKey = challengeCodeSupportMapper.getChallengeCodeKey(entityKey);
    challengeCodeSupportMapper.updateChallengeCodeKey(entityKey, null);
    challengeCodeMapper.deleteChallengeCode(challengeCodeKey);
  }

  public UUID getChallengeCode(Integer entityKey) {
    return challengeCodeMapper.getChallengeCode(challengeCodeSupportMapper.getChallengeCodeKey(entityKey));
  }

}
