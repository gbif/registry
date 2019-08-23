package org.gbif.registry.persistence;

import org.apache.ibatis.annotations.Param;

/**
 * Support for ChallengeCode based surety inside a Mapper. The implementation of this interface allows an entity
 * to be used with the ChallengeCodeManager.
 */
// TODO: 2019-07-01 Invalid bound statement (not found), moved to parent package for now
public interface ChallengeCodeSupportMapper<K> {

  Integer getChallengeCodeKey(@Param("key") K key);

  boolean updateChallengeCodeKey(@Param("key") K key, @Param("challengeCodeKey") Integer challengeCodeKey);
}
