package org.gbif.registry.surety.persistence;

import org.apache.ibatis.annotations.Param;

/**
 * Support for ChallengeCode based surety inside a Mapper. The implementation of this interface allows an entity
 * to be used with the {@link ChallengeCodeManager}.
 */
public interface ChallengeCodeSupportMapper<K> {

  Integer getChallengeCodeKey(@Param("key") K key);
  boolean updateChallengeCodeKey(@Param("key") K key, @Param("challengeCodeKey")Integer challengeCodeKey);

}
