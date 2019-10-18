package org.gbif.registry.surety.persistence;

import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.surety.ChallengeCodeMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Qualifier("organizationChallengeCodeManager")
@Service
public class OrganizationChallengeCodeManager extends BaseChallengeCodeManager<UUID> {

  public OrganizationChallengeCodeManager(ChallengeCodeMapper challengeCodeMapper,
                                          OrganizationMapper organizationMapper) {
    super(challengeCodeMapper, organizationMapper);
  }
}
