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
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.PostPersist;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.registry.identity.model.ModelMutationError;
import org.gbif.registry.identity.model.PropertyConstants;
import org.gbif.registry.identity.model.UserModelMutationResult;
import org.gbif.registry.identity.util.RegistryPasswordEncoder;
import org.gbif.registry.persistence.mapper.UserMapper;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.groups.Default;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Strings;

import static org.gbif.registry.identity.model.UserModelMutationResult.withError;
import static org.gbif.registry.identity.model.UserModelMutationResult.withSingleConstraintViolation;

/**
 * Main implementation of {@link IdentityService} on top of mybatis. Notes: - usernames are case
 * insensitive but the constraint in the db only allows lowercase at the moment (could be removed) -
 * emails are stored as provided (the case is preserved) but are queried in lowercase
 */
@Service
public class IdentityServiceImpl implements IdentityService {

  private final UserMapper userMapper;
  private final UserSuretyDelegate userSuretyDelegate;
  private final Validator validator;

  private static final Range<Integer> PASSWORD_LENGTH_RANGE = Range.between(6, 256);

  private static final UnaryOperator<String> NORMALIZE_USERNAME_FCT = StringUtils::trim;
  private static final UnaryOperator<String> NORMALIZE_EMAIL_FCT =
      email -> Optional.ofNullable(email).map(String::trim).orElse(null);

  private static final RegistryPasswordEncoder PASSWORD_ENCODER = new RegistryPasswordEncoder();

  @Autowired
  public IdentityServiceImpl(
      UserMapper userMapper, UserSuretyDelegate userSuretyDelegate, Validator validator) {
    this.userMapper = userMapper;
    this.userSuretyDelegate = userSuretyDelegate;
    this.validator = validator;
  }

  @Override
  @Transactional
  public UserModelMutationResult create(GbifUser rawUser, String password) {
    GbifUser user = normalize(rawUser);
    if (userMapper.get(user.getUserName()) != null
        || userMapper.getByEmail(user.getEmail()) != null) {
      return withError(ModelMutationError.USER_ALREADY_EXIST);
    }

    if (StringUtils.isBlank(password) || !PASSWORD_LENGTH_RANGE.contains(password.length())) {
      return withError(ModelMutationError.PASSWORD_LENGTH_VIOLATION);
    }

    user.setPasswordHash(PASSWORD_ENCODER.encode(password));

    Optional<UserModelMutationResult> beanValidation = validateBean(user, PrePersist.class);
    if (beanValidation.isPresent()) {
      return beanValidation.get();
    }
    userMapper.create(user);

    // trigger email
    userSuretyDelegate.onNewUser(user);

    return UserModelMutationResult.onSuccess(user.getUserName(), user.getEmail());
  }

  @Override
  public UserModelMutationResult update(GbifUser rawUser) {
    GbifUser user = normalize(rawUser);
    return Optional.ofNullable(getByKey(user.getKey()))
        .map(
            currentUser -> {
              // handle email change and user if the user want to change it is is not already
              // linked to another account
              Optional<GbifUser> gbifUserAlreadyUsingEmail =
                  Optional.of(currentUser)
                      .filter(u -> !u.getEmail().equalsIgnoreCase(user.getEmail()))
                      .map(u -> userMapper.getByEmail(user.getEmail()));

              if (gbifUserAlreadyUsingEmail.isPresent()) {
                return withError(ModelMutationError.EMAIL_ALREADY_IN_USE);
              }

              Optional<UserModelMutationResult> beanValidation =
                  validateBean(user, PostPersist.class);
              if (beanValidation.isPresent()) {
                return beanValidation.get();
              }

              userMapper.update(user);
              return UserModelMutationResult.onSuccess(user.getUserName(), user.getEmail());
            })
        .orElse(null);
  }

  /**
   * Runs a Java bean validation on the provided {@link GbifUser} and a scope (e.g.
   * PostPersist.class)
   *
   * @param gbifUser
   * @param scope
   * @return
   */
  private Optional<UserModelMutationResult> validateBean(GbifUser gbifUser, Class<?> scope) {
    Set<ConstraintViolation<GbifUser>> violations =
        validator.validate(gbifUser, scope, Default.class);
    return violations.isEmpty() ? Optional.empty() : Optional.of(withError(violations));
  }

  @Override
  public void delete(int userKey) {
    userMapper.delete(userKey);
  }

  @Override
  public GbifUser getByKey(int key) {
    return userMapper.getByKey(key);
  }

  @Override
  public GbifUser getBySystemSetting(String key, String value) {
    return userMapper.getBySystemSetting(key, value);
  }

  /**
   * Get a {@link GbifUser} using its identifier (username or email). The username is case
   * insensitive.
   *
   * @param identifier
   * @return {@link GbifUser} or null
   */
  @Override
  public GbifUser get(String identifier) {
    if (Strings.isNullOrEmpty(identifier)) {
      return null;
    }
    // this assumes username name can not contains @ (which is the case, see AbstractGbifUser's
    // getUserName())
    return StringUtils.contains(identifier, "@")
        ? getByEmail(identifier)
        : userMapper.get(NORMALIZE_USERNAME_FCT.apply(identifier));
  }

  /**
   * Get a {@link GbifUser} using its email. The email is case insensitive.
   *
   * @param email
   * @return {@link GbifUser} or null
   */
  private GbifUser getByEmail(String email) {
    // emails are stored in lowercase
    // the mybatis mapper will run the query with a lower()
    return userMapper.getByEmail(NORMALIZE_EMAIL_FCT.apply(email));
  }

  @Override
  public PagingResponse<GbifUser> list(@Nullable Pageable pageable) {
    return search(null, pageable);
  }

  @Override
  public PagingResponse<GbifUser> search(@Nullable String query, @Nullable Pageable pageable) {
    return pagingResponse(pageable, userMapper.count(query), userMapper.search(query, pageable));
  }

  /**
   * Authenticate a user
   *
   * @param username username or email address
   * @param password clear text password
   * @return
   */
  @Override
  public GbifUser authenticate(String username, String password) {
    if (Strings.isNullOrEmpty(username) || password == null) {
      return null;
    }

    // use the settings which are the prefix in the existing password hash to encode the provided
    // password
    // and verify that they result in the same

    // ensure there is no pending challenge code, unless the user already logged in in the past.
    // If the user logged in in the past we assume the challengeCode is from a reset password and
    // since we
    // can't be sure the user initiated the request himself we must allow to login
    return Optional.ofNullable(get(username))
        .filter(
            user ->
                PASSWORD_ENCODER
                        .encode(password, user.getPasswordHash())
                        .equalsIgnoreCase(user.getPasswordHash())
                    && (!userSuretyDelegate.hasChallengeCode(user.getKey())
                        || user.getLastLogin() != null))
        .orElse(null);
  }

  @Override
  public void updateLastLogin(int userKey) {
    userMapper.updateLastLogin(userKey);
  }

  @Override
  public boolean hasPendingConfirmation(int userKey) {
    return userSuretyDelegate.hasChallengeCode(userKey);
  }

  @Override
  public boolean isConfirmationKeyValid(int userKey, UUID confirmationKey) {
    return userSuretyDelegate.isValidChallengeCode(userKey, confirmationKey);
  }

  @Override
  public boolean confirmUser(int userKey, UUID confirmationKey) {
    return Optional.ofNullable(confirmationKey)
        .map(
            confirmationKeyVal ->
                userSuretyDelegate.confirmUser(getByKey(userKey), confirmationKeyVal))
        .orElse(Boolean.FALSE);
  }

  @Override
  public void resetPassword(int userKey) {
    // ensure the user exists
    Optional.ofNullable(userMapper.getByKey(userKey))
        .ifPresent(userSuretyDelegate::onPasswordReset);
  }

  @Override
  public UserModelMutationResult updatePassword(
      int userKey, String newPassword, UUID challengeCode) {
    return confirmUser(userKey, challengeCode)
        ? updatePassword(userKey, newPassword)
        : withSingleConstraintViolation(
            PropertyConstants.CHALLENGE_CODE_PROPERTY_NAME, PropertyConstants.CONSTRAINT_INCORRECT);
  }

  @Override
  public UserModelMutationResult updatePassword(int userKey, String newPassword) {
    return Optional.ofNullable(userMapper.getByKey(userKey))
        .map(
            user -> {
              if (StringUtils.isBlank(newPassword)
                  || !PASSWORD_LENGTH_RANGE.contains(newPassword.length())) {
                return withError(ModelMutationError.PASSWORD_LENGTH_VIOLATION);
              }
              user.setPasswordHash(PASSWORD_ENCODER.encode(newPassword));
              userMapper.update(user);
              return UserModelMutationResult.onSuccess();
            })
        .orElse(withSingleConstraintViolation("user", PropertyConstants.CONSTRAINT_UNKNOWN));
  }

  /**
   * The main purpose of this method is to normalize the content of some fields from a {@link
   * GbifUser}. The goal is to ensure we can query this object in the same way we handle
   * inserts/updates. - trim() on username - trim() + toLowerCase() on emails
   *
   * @param gbifUser
   * @return
   */
  private static GbifUser normalize(GbifUser gbifUser) {
    gbifUser.setUserName(NORMALIZE_USERNAME_FCT.apply(gbifUser.getUserName()));
    gbifUser.setEmail(NORMALIZE_EMAIL_FCT.apply(gbifUser.getEmail()));
    return gbifUser;
  }

  /**
   * Null safe builder to construct a paging response.
   *
   * @param page page to create response for, can be null
   */
  private static PagingResponse<GbifUser> pagingResponse(
      @Nullable Pageable page, long count, List<GbifUser> result) {
    return new PagingResponse<>(page == null ? new PagingRequest() : page, count, result);
  }

  @Override
  public List<UUID> listEditorRights(String userName) {
    return userMapper.listEditorRights(userName);
  }

  @Override
  public void addEditorRight(String userName, UUID key) {
    userMapper.addEditorRight(userName, key);
  }

  @Override
  public void deleteEditorRight(String userName, UUID key) {
    userMapper.deleteEditorRight(userName, key);
  }
}
