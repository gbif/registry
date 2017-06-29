package org.gbif.identity.service;

import org.gbif.api.model.common.User;

import java.util.UUID;

/**
 *
 */
public interface UserSuretyService {

  boolean hasChallengeCode(Integer userKey);

  boolean isValidChallengeCode(Integer userKey, UUID challengeCode);

  /**
   * After a user is created, this function is responsible to send an email to the provided user.
   * The email address used will be the one returned by {@code user.getEmail()}.
   *
   * @param user
   */
  void onNewUser(User user);

  /**
   *
   * @param key
   * @param confirmationObject object used to handle confirmation by the implementation.
   * @return
   */
  boolean confirmUser(Integer key, UUID confirmationObject);

  /**
   * When we receive a rest password request, this function is responsible to send an email to the provided user.
   * The email address used will be the one returned by {@code user.getEmail()}.
   *
   * @param user
   */
  void onPasswordReset(User user);

}
