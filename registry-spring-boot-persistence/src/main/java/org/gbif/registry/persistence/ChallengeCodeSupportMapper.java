package org.gbif.registry.persistence;

/**
 * Support for ChallengeCode based surety inside a Mapper. The implementation of this interface allows an entity
 * to be used with the ChallengeCodeManager.
 */
// TODO: 2019-07-01 Invalid bound statement (not found), moved to parent package for now
public interface ChallengeCodeSupportMapper<K> {

  Integer getChallengeCodeKey(K key);

  boolean updateChallengeCodeKey(K key, Integer challengeCodeKey);
}
