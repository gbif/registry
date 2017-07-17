package org.gbif.identity.mybatis;

import org.gbif.registry.surety.persistence.ChallengeCodeMapper;
import org.gbif.registry.surety.persistence.ChallengeCodeSupportMapper;

import java.util.UUID;

/**
 * Helper class that offers direct access to challenge codes for testing purpose (only).
 */
public class IdentitySuretyTestHelper {

  private ChallengeCodeMapper challengeCodeMapper;
  private ChallengeCodeSupportMapper<Integer> challengeCodeSupportMapper;

  public IdentitySuretyTestHelper(ChallengeCodeMapper challengeCodeMapper,
                           ChallengeCodeSupportMapper<Integer> challengeCodeSupportMapper) {
    this.challengeCodeMapper = challengeCodeMapper;
    this.challengeCodeSupportMapper = challengeCodeSupportMapper;
  }

  public UUID getChallengeCode(Integer entityKey) {
    return challengeCodeMapper.getChallengeCode(challengeCodeSupportMapper.getChallengeCodeKey(entityKey));
  }

}
