package org.gbif.identity.email;

import org.gbif.api.model.common.User;

import java.util.UUID;

/**
 * Manager of emails related tasks.
 */
public interface IdentityEmailManager {


  /**
   * After a user is created, this function is responsible to send an email to the provided user.
   * The email address used will be the one returned by {@code user.getEmail()}.
   *
   * @param user
   * @param challengeCode
   */
  void generateAndSendUserCreated(User user, UUID challengeCode);

}
