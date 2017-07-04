package org.gbif.identity.service;

import org.gbif.api.model.common.GbifUser;

import java.util.UUID;

/**
 *
 */
interface UserSuretyDelegateIf {

  boolean hasChallengeCode(Integer userKey);
  boolean isValidChallengeCode(Integer userKey, UUID challengeCode);
  void onNewUser(GbifUser user);
  boolean confirmUser(Integer key, UUID confirmationObject);
  void onPasswordReset(GbifUser user);
}
