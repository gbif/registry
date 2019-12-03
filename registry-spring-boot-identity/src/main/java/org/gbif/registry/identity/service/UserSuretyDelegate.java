package org.gbif.registry.identity.service;

import org.gbif.api.model.common.GbifUser;

import java.util.UUID;

/**
 * Internal class used to coordinates actions between the MyBatis layer and the email manager.
 */
interface UserSuretyDelegate {

  /**
   * Check if user has a challenge code.
   */
  boolean hasChallengeCode(Integer userKey);

  /**
   * Check if user's challenge code is valid.
   *
   * @param userKey       user key (id)
   * @param challengeCode challenge code (confirmation object)
   * @return true if challenge code is valid and false otherwise
   */
  boolean isValidChallengeCode(Integer userKey, UUID challengeCode);

  /**
   * Handles the logic on new user (e.g. create challenge code, generate and send email).
   *
   * @param user new user
   */
  void onNewUser(GbifUser user);

  /**
   * Handles the logic on user confirmation (e.g. confirm user, generate and send email).
   *
   * @param user               user to confirm
   * @param confirmationObject confirmation object (challenge code)
   * @return true if user was confirmed and false otherwise
   */
  boolean confirmUser(GbifUser user, UUID confirmationObject);

  /**
   * Handles the logic on password resetting (e.g. create challenge code, generate and send email).
   *
   * @param user user to reset password
   */
  void onPasswordReset(GbifUser user);
}
