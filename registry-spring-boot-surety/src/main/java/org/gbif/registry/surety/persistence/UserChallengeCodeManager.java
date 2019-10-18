package org.gbif.registry.surety.persistence;

import org.gbif.registry.persistence.mapper.UserMapper;
import org.gbif.registry.persistence.mapper.surety.ChallengeCodeMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Qualifier("userChallengeCodeManager")
@Service
public class UserChallengeCodeManager extends BaseChallengeCodeManager<Integer> {

  public UserChallengeCodeManager(ChallengeCodeMapper challengeCodeMapper,
                                  UserMapper userMapper) {
    super(challengeCodeMapper, userMapper);
  }
}
