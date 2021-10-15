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
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.occurrence.Download;
import org.gbif.api.model.registry.PostPersist;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.identity.model.ModelMutationError;
import org.gbif.registry.identity.model.PropertyConstants;
import org.gbif.registry.identity.model.UserModelMutationResult;
import org.gbif.registry.identity.util.RegistryPasswordEncoder;
import org.gbif.registry.persistence.mapper.UserMapper;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.groups.Default;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Strings;

import static org.gbif.registry.identity.model.UserModelMutationResult.withError;
import static org.gbif.registry.identity.model.UserModelMutationResult.withSingleConstraintViolation;
import static org.gbif.registry.identity.util.IdentityUtils.NORMALIZE_EMAIL_FCT;
import static org.gbif.registry.identity.util.IdentityUtils.NORMALIZE_USERNAME_FCT;

/**
 * Main implementation of {@link IdentityService} on top of mybatis. Notes: - usernames are case
 * insensitive but the constraint in the db only allows lowercase at the moment (could be removed) -
 * emails are stored as provided (the case is preserved) but are queried in lowercase
 */
@Primary
@Service
public class IdentityServiceImpl extends BaseIdentityAccessService implements IdentityService {

  private final UserMapper userMapper;
  private final UserSuretyDelegate userSuretyDelegate;
  private final Validator validator;

  private static final Range<Integer> PASSWORD_LENGTH_RANGE = Range.between(6, 256);

  private static final RegistryPasswordEncoder PASSWORD_ENCODER = new RegistryPasswordEncoder();

  @Autowired
  public IdentityServiceImpl(
      UserMapper userMapper, UserSuretyDelegate userSuretyDelegate, Validator validator) {
    super(userMapper);
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
    GbifUser currentUser = getByKey(user.getKey());

    if (currentUser != null) {
      boolean isEmailChanged = !user.getEmail().equalsIgnoreCase(currentUser.getEmail());

      if (isEmailChanged) {
        // handle email change and user if the user want to change it
        // and it is not already linked to another account
        GbifUser userByEmail = userMapper.getByEmail(user.getEmail());

        if (userByEmail != null) {
          return withError(ModelMutationError.EMAIL_ALREADY_IN_USE);
        } else {
          // copy settings if locale was updated
          currentUser.setSettings(user.getSettings());
          userSuretyDelegate.onChangeEmail(currentUser, user.getEmail());
        }

        // email must be updated separately by clicking a email's link
        user.setEmail(currentUser.getEmail());
      }

      Optional<UserModelMutationResult> beanValidation = validateBean(user, PostPersist.class);
      if (beanValidation.isPresent()) {
        return beanValidation.get();
      }

      userMapper.update(user);
      return UserModelMutationResult.onSuccess(user.getUserName(), user.getEmail());
    }

    return null;
  }

  /**
   * Runs a Java bean validation on the provided {@link GbifUser} and a scope (e.g.
   * PostPersist.class)
   *
   * @param gbifUser user to validate
   * @param scope validation scope
   * @return validation result
   */
  private Optional<UserModelMutationResult> validateBean(GbifUser gbifUser, Class<?> scope) {
    Set<ConstraintViolation<GbifUser>> violations =
        validator.validate(gbifUser, scope, Default.class);
    return violations.isEmpty() ? Optional.empty() : Optional.of(withError(violations));
  }

  @Override
  public void delete(int userKey) {
    userMapper.deleteByKey(userKey);
  }

  @Override
  public void delete(GbifUser userBefore, GbifUser user, List<Download> downloads) {
    userMapper.delete(user);
    userSuretyDelegate.onDeleteUser(userBefore, downloads);
  }

  @Override
  public GbifUser getByKey(int key) {
    return userMapper.getByKey(key);
  }

  @Override
  public GbifUser getBySystemSetting(String key, String value) {
    return userMapper.getBySystemSetting(key, value);
  }

  @Override
  public PagingResponse<GbifUser> list(@Nullable Pageable pageable) {
    return search(null, null, null, null, null, pageable);
  }

  @Override
  public PagingResponse<GbifUser> search(
      @Nullable String query,
      Set<UserRole> roles,
      @Nullable Set<UUID> editorRightsOn,
      @Nullable Set<String> namespaceRightsOn,
      @Nullable Set<Country> countryRightsOn,
      @Nullable Pageable pageable) {
    return pagingResponse(
        pageable,
        userMapper.count(query, roles, editorRightsOn, namespaceRightsOn, countryRightsOn),
        userMapper.search(
            query, roles, editorRightsOn, namespaceRightsOn, countryRightsOn, pageable));
  }

  /**
   * Authenticate a user
   *
   * @param username username or email address
   * @param password clear text password
   * @return authenticated user
   */
  @Override
  @Nullable
  public GbifUser authenticate(String username, String password) {
    if (Strings.isNullOrEmpty(username) || password == null) {
      return null;
    }

    // use the settings which are the prefix in the existing password hash to encode the provided
    // password and verify that they result in the same

    // ensure there is no pending challenge code, unless the user already logged in in the past.
    // If the user logged in in the past we assume the challengeCode is from a reset password and
    // since we can't be sure the user initiated the request himself we must allow to login
    GbifUser user = get(username);

    if (user != null
        && PASSWORD_ENCODER
            .encode(password, user.getPasswordHash())
            .equalsIgnoreCase(user.getPasswordHash())
        && (!userSuretyDelegate.hasChallengeCode(user.getKey()) || user.getLastLogin() != null)) {
      return user;
    }

    return null;
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
  public boolean isConfirmationKeyValid(int userKey, String email, UUID confirmationKey) {
    return userSuretyDelegate.isValidChallengeCode(userKey, email, confirmationKey);
  }

  @Override
  public boolean confirmUser(int userKey, UUID confirmationKey) {
    if (confirmationKey != null) {
      return userSuretyDelegate.confirmUser(getByKey(userKey), confirmationKey);
    }

    return false;
  }

  @Override
  public boolean confirmUserAndEmail(int userKey, String email, UUID confirmationKey) {
    if (confirmationKey != null) {
      return userSuretyDelegate.confirmUserAndEmail(getByKey(userKey), email, confirmationKey);
    }

    return false;
  }

  @Override
  public boolean confirmAndNotifyUser(int userKey, UUID confirmationKey) {
    if (confirmationKey != null) {
      return userSuretyDelegate.confirmAndNotifyUser(getByKey(userKey), confirmationKey);
    }

    return false;
  }

  @Override
  public void resetPassword(int userKey) {
    GbifUser user = userMapper.getByKey(userKey);

    if (user != null) {
      userSuretyDelegate.onPasswordReset(user);
    }
  }

  @Override
  public UserModelMutationResult updateEmail(
      int userKey, String oldEmail, String newEmail, UUID confirmationKey) {
    UserModelMutationResult result;

    GbifUser userByEmail = userMapper.getByEmail(newEmail);
    if (userByEmail != null) {
      result = withError(ModelMutationError.EMAIL_ALREADY_IN_USE);
    } else if (confirmUserAndEmail(userKey, newEmail, confirmationKey)) {
      GbifUser user = userMapper.getByKey(userKey);

      if (user != null) {
        user.setEmail(newEmail);
        userMapper.update(user);
        userSuretyDelegate.onEmailChanged(user, oldEmail);

        return UserModelMutationResult.onSuccess();
      } else {
        result = withSingleConstraintViolation("user", PropertyConstants.CONSTRAINT_UNKNOWN);
      }
    } else {
      result =
          withSingleConstraintViolation(
              PropertyConstants.CHALLENGE_CODE_PROPERTY_NAME,
              PropertyConstants.CONSTRAINT_INCORRECT);
    }

    return result;
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
    GbifUser user = userMapper.getByKey(userKey);

    if (user != null) {
      if (StringUtils.isBlank(newPassword)
          || !PASSWORD_LENGTH_RANGE.contains(newPassword.length())) {
        return withError(ModelMutationError.PASSWORD_LENGTH_VIOLATION);
      }

      user.setPasswordHash(PASSWORD_ENCODER.encode(newPassword));
      userMapper.update(user);
      userSuretyDelegate.onPasswordChanged(user);

      return UserModelMutationResult.onSuccess();
    }

    return withSingleConstraintViolation("user", PropertyConstants.CONSTRAINT_UNKNOWN);
  }

  /**
   * The main purpose of this method is to normalize the content of some fields from a {@link
   * GbifUser}. The goal is to ensure we can query this object in the same way we handle
   * inserts/updates. - trim() on username - trim() + toLowerCase() on emails
   *
   * @param gbifUser user to normalize
   * @return normalized user
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

  @Override
  public List<String> listNamespaceRights(String userName) {
    return userMapper.listNamespaceRights(userName);
  }

  @Override
  public void addNamespaceRight(String userName, String namespace) {
    userMapper.addNamespaceRight(userName, namespace);
  }

  @Override
  public void deleteNamespaceRight(String userName, String namespace) {
    userMapper.deleteNamespaceRight(userName, namespace);
  }

  @Override
  public List<Country> listCountryRights(String userName) {
    return userMapper.listCountryRights(userName);
  }

  @Override
  public void addCountryRight(String userName, Country country) {
    userMapper.addCountryRight(userName, country);
  }

  @Override
  public void deleteCountryRight(String userName, Country country) {
    userMapper.deleteCountryRight(userName, country);
  }
}
