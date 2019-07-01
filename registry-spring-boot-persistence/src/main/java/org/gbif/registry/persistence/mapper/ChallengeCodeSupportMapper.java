package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * Support for ChallengeCode based surety inside a Mapper. The implementation of this interface allows an entity
 * to be used with the ChallengeCodeManager.
 */
public interface ChallengeCodeSupportMapper<K> {

  // TODO: 2019-06-26 OrganizationMapper, UserMapper
  Integer getChallengeCodeKey(@Param("key") K key);

  boolean updateChallengeCodeKey(@Param("key") K key, @Param("challengeCodeKey") Integer challengeCodeKey);
}
