package org.gbif.registry.surety.persistence;

import org.gbif.registry.surety.model.ChallengeCode;

import java.util.UUID;

import org.apache.ibatis.annotations.Param;

/**
 * Mapper for ChallengeCode table
 */
public interface ChallengeCodeMapper {

  void createChallengeCode(ChallengeCode challengeCode);
  UUID getChallengeCode(@Param("key") Integer key);

  void deleteChallengeCode(int key);

}
