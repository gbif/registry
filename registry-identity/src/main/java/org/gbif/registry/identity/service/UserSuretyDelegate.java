/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.identity.service;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.occurrence.Download;

import java.util.List;
import java.util.UUID;

/** Internal class used to coordinate actions between the MyBatis layer and the email manager. */
public interface UserSuretyDelegate {

  /** Check if user has a challenge code. */
  boolean hasChallengeCode(Integer userKey);

  /**
   * Check if challenge code is valid for user and email.
   *
   * @param userKey user key (id)
   * @param email email
   * @param challengeCode challenge code (confirmation object)
   * @return true if challenge code is valid and false otherwise
   */
  boolean isValidChallengeCode(Integer userKey, String email, UUID challengeCode);

  /**
   * Handles the logic on new user (e.g. create challenge code, generate and send email).
   *
   * @param user new user
   */
  void onNewUser(GbifUser user);

  /**
   * Handles the logic on user confirmation.
   *
   * @param user user to confirm
   * @param confirmationObject confirmation object (challenge code)
   * @return true if user was confirmed and false otherwise
   */
  boolean confirmUser(GbifUser user, UUID confirmationObject);

  /**
   * Handles the logic on user confirmation (e.g. confirm user, generate and send email).
   *
   * @param user user to confirm
   * @param confirmationObject confirmation object (challenge code)
   * @return true if user was confirmed and false otherwise
   */
  boolean confirmAndNotifyUser(GbifUser user, UUID confirmationObject);

  /**
   * Handles the logic on user confirmation and email confirmation.
   *
   * @param user user to confirm
   * @param email email to confirm
   * @param confirmationObject confirmation object (challenge code)
   * @return true if user was confirmed and false otherwise
   */
  boolean confirmUserAndEmail(GbifUser user, String email, UUID confirmationObject);

  /**
   * Handles the logic on user deletion.
   *
   * @param user user
   * @param downloads user's downloads
   */
  void onDeleteUser(GbifUser user, List<Download> downloads);

  /**
   * Handles the logic on password resetting (e.g. create challenge code, generate and send email).
   *
   * @param user user to reset password
   */
  void onPasswordReset(GbifUser user);

  /**
   * Handles the logic on password changed.
   *
   * @param user user who password was changed
   */
  void onPasswordChanged(GbifUser user);

  /**
   * Handles the logic on change email.
   *
   * @param user user who email has to be changed
   * @param newEmail new email address
   */
  void onChangeEmail(GbifUser user, String newEmail);

  /**
   * Handles the logic on email changed.
   *
   * @param user user who email was changed
   * @param oldEmail old email address
   */
  void onEmailChanged(GbifUser user, String oldEmail);
}
