package org.gbif.registry.identity.service;

import org.gbif.api.model.common.GbifUser;

import java.util.UUID;

/**
 * Internal class used to coordinates actions between the MyBatis layer and the email manager.
 *
 */
interface UserSuretyDelegate {

  boolean hasChallengeCode(Integer userKey);
  boolean isValidChallengeCode(Integer userKey, UUID challengeCode);
  void onNewUser(GbifUser user);
  boolean confirmUser(GbifUser user, UUID confirmationObject);
  void onPasswordReset(GbifUser user);
}
