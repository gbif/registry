/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
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
package org.gbif.registry.identity.service;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.service.common.IdentityAccessService;
import org.gbif.registry.identity.model.UserModelMutationResult;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * The identity (management) service provides means to create, update and delete User accounts, and
 * provide the mechanisms to authenticate a user with their password. This is a replacement of the
 * deprecated UserService which was a read only service, backed by a managed database (Drupal) and
 * provides a writable option.
 *
 * <p>Design and implementation decisions: - Create method returns result objects (e.g. {@link
 * UserModelMutationResult}) instead of throwing exceptions - Authorization related to the {@link
 * IdentityService} itself (who is allowed to create user ...) is NOT done by this service.
 */
public interface IdentityService extends IdentityAccessService {

  /**
   * Exposes the user by primary key instead of the username. This method will return the user even
   * if he is deleted.
   *
   * @param id The primary key of the user
   * @return The user or null
   */
  @Nullable
  GbifUser getByKey(int id);

  /**
   * Get a user by systemSettings (e.g. Orcid login). The key and the value shall match.
   *
   * @param key key of the systemSetting to check
   * @param value value of the systemSetting to check
   * @return The user or null
   */
  @Nullable
  GbifUser getBySystemSetting(String key, String value);

  /**
   * Checks if a user requires a confirmation. Confirmation can be for a new user or a password
   * change.
   *
   * @return the user has a confirmation pending.
   */
  boolean hasPendingConfirmation(int userKey);

  /**
   * A simple search that supports paging.
   *
   * @return a pageable response of network entities, with accurate counts.
   */
  PagingResponse<GbifUser> search(String query, @Nullable Pageable page);

  /**
   * Create a new user.
   *
   * @param user {@link GbifUser} to be created.
   * @return result of the user creation
   */
  UserModelMutationResult create(GbifUser user, String password);

  /**
   * Apply an update to a user. It is the responsibility of the caller to ensure what is allowed to
   * be changed (e.g. base on the roles).
   */
  UserModelMutationResult update(GbifUser user);

  void delete(int userKey);

  /**
   * Delete a user and remove all sensitive data.
   *
   * @param userBefore user (with sensitive data)
   * @param user user to delete (without sensitive data)
   * @param downloads user's downloads
   */
  void delete(GbifUser userBefore, GbifUser user, List<Download> downloads);

  PagingResponse<GbifUser> list(@Nullable Pageable var1);

  /**
   * Trigger an update of the last login date.
   */
  void updateLastLogin(int userKey);

  /**
   * Check if a confirmationKey is valid for a specific user.
   *
   * @return the confirmationKey is valid or not
   */
  boolean isConfirmationKeyValid(int userKey, UUID confirmationKey);

  /**
   * Confirms user using a confirmation key. A confirmationKey can only be confirmed once and only
   * if it was previously assigned. If no confirmationKey is present this method will return false;
   *
   * @return the user was confirmed by this action or not
   */
  boolean confirmUser(int userKey, UUID confirmationKey, boolean emailEnabled);

  /**
   * Allows to change the password of a user providing a challenge code instead of its password. A
   * challenge code can only be used once and only if it was previously assigned (it assumes {@code
   * resetPassword} was previously called). If no challenge code is present this method will return
   * false and the password won't be changed.
   */
  UserModelMutationResult updatePassword(int userKey, String newPassword, UUID confirmationKey);

  /**
   * Allows to change the password of a user that is already authenticated.
   */
  UserModelMutationResult updatePassword(int userKey, String newPassword);

  void resetPassword(int userKey);

  UserModelMutationResult updateEmail(
      int userKey, String oldEmail, String newEmail, UUID confirmationKey);

  /**
   * Lists the entity keys the user has editor permissions on.
   */
  List<UUID> listEditorRights(String userName);

  /**
   * Grant the user rights over the given entity.
   */
  void addEditorRight(String userName, UUID key);

  /**
   * Remove rights from the given entity for the user.
   */
  void deleteEditorRight(String userName, UUID key);
}
