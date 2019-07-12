package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.ChallengeCode;
import org.springframework.stereotype.Repository;

/**
 * Mapper for ChallengeCode table
 */
@Repository
public interface ChallengeCodeMapper {

  void createChallengeCode(ChallengeCode challengeCode);

  String getChallengeCode(@Param("key") Integer key);

  void deleteChallengeCode(int key);
}
