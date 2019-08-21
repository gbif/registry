package org.gbif.registry.persistence.mapper.surety;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.ChallengeCode;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Mapper for ChallengeCode table
 */
@Repository
public interface ChallengeCodeMapper {

  void createChallengeCode(ChallengeCode challengeCode);

  UUID getChallengeCode(@Param("key") Integer key);

  void deleteChallengeCode(int key);
}
