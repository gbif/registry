/*
 * Copyright 2014 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.api.service.common;

import org.gbif.api.model.common.User;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.identity.model.Session;
import org.gbif.identity.model.UserModelMutationResult;

import java.util.UUID;
import javax.annotation.Nullable;

/**
 * The identity service provides means to create, update and delete User accounts, and provide the mechanisms to
 * authenticate a user with their password.
 * This is a replacement of the deprecated UserService which was a read only service, backed by a managed database
 * (Drupal) and provides a writable option.
 *
 * Design and implementation decisions:
 * - This service is also responsible to handle sessions
 * - Create method returns result objects (e.g. {@link UserModelMutationResult}) instead of throwing exceptions
 * - Authorization related to the {@link IdentityService} itself (who is allowed to create user ...) is NOT done by this service.
 *
 */
public interface IdentityService {

  /**
   * Exposes the user by primary key instead of the username.
   * This method will return the user even if he is deleted.
   * @param id The primary key of the user
   * @return The user or null
   */
  @Nullable
  User getByKey(int id);

  @Nullable
  User get(String userName);

  @Nullable
  User getByEmail(String email);

  /**
   * Get a user by identifier. An identifier is a username OR an email.
   *
   * @param identifier
   *
   * @return The user or null
   */
  @Nullable
  User getByIdentifier(String identifier);


  /**
   * Authenticates a user.
   * @param password clear text password
   *
   * @return the authenticated user or null if not found or wrong credentials provided
   */
  @Nullable
  User authenticate(String username, String password);

  /**
   * Retrieves a user by a currently open login session.
   * The session name is stored by in a cookie.
   * @param session the session name as found in the cookie
   * @return the user of an existing session or NULL if not found
   */
  @Nullable
  User getBySession(String session);

  /**
   * A simple search that supports paging.
   *
   * @return a pageable response of network entities, with accurate counts.
   */
  PagingResponse<User> search(String query, @Nullable Pageable page);

  /**
   * Create a new user.
   * @param user {@link User} to be created.
   * @return result of the user creation
   */
  UserModelMutationResult create(User user, String password);

  /**
   * Apply an update to a user.
   * It is the responsibility of the caller to ensure what is allowed to be changed (e.g. base on the roles).
   * @param user
   * @return
   */
  UserModelMutationResult update(User user);

  void delete(int userKey);

  PagingResponse<User> list(@Nullable Pageable var1);

  Session createSession(String username);
  void terminateSession(String session);
  void terminateAllSessions(String username);

  /**
   * Trigger an update of the last login date.
   * @param userKey
   */
  void updateLastLogin(int userKey);

  /**
   * Check if a challenge code is valid  for a specific user.
   *
   * @param userKey
   * @param challengeCode
   *
   * @return the challenge is valid or not
   */
  boolean isChallengeCodeValid(int userKey, UUID challengeCode);

  /**
   * Confirms a challenge code for a specific user. A challenge code can only be confirmed once and only if it was
   * previously assigned. If no challenge code is present this method will return false;
   *
   * @param userKey
   * @param challengeCode
   *
   * @return the challenge was confirmed or not
   */
  boolean confirmChallengeCode(int userKey, UUID challengeCode);

  /**
   * Checks if we have a challenge code stored for a specific user.
   * @param userKey
   * @return the user has a challenge code stored
   */
  boolean containsChallengeCode(int userKey);

  /**
   * Allows to change the password of a user providing a challenge code instead of its password.
   * A challenge code can only be used once and only if it was
   * previously assigned (it assumes {@code resetPassword} was previously called).
   * If no challenge code is present this method will return false and the password won't be changed.
   *
   * @param userKey
   * @param newPassword
   * @param challengeCode
   *
   * @return the password was updated or not
   */
  boolean updatePassword(int userKey, String newPassword, UUID challengeCode);


  void resetPassword(int userKey);
}
