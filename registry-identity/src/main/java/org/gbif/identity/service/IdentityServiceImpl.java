package org.gbif.identity.service;

import org.gbif.api.model.common.User;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.PostPersist;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.service.common.IdentityService;
import org.gbif.identity.email.IdentityEmailManager;
import org.gbif.identity.model.ModelMutationError;
import org.gbif.identity.model.PropertyConstants;
import org.gbif.identity.model.UserModelMutationResult;
import org.gbif.identity.mybatis.UserMapper;
import org.gbif.identity.util.PasswordEncoder;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.groups.Default;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import org.apache.bval.jsr303.ApacheValidationProvider;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.guice.transactional.Transactional;

import static org.gbif.identity.model.UserModelMutationResult.withError;
import static org.gbif.identity.model.UserModelMutationResult.withSingleConstraintViolation;


/**
 * Main implementation of {@link IdentityService} on top of mybatis.
 */
class IdentityServiceImpl implements IdentityService {

  private final UserMapper userMapper;
  private final IdentityEmailManager identityEmailManager;

  private final Range<Integer> PASSWORD_LENGTH_RANGE = Range.between(6, 256);

  private static final Validator BEAN_VALIDATOR =
          Validation.byProvider(ApacheValidationProvider.class)
                  .configure()
                  .buildValidatorFactory()
                  .getValidator();

  private final PasswordEncoder encoder = new PasswordEncoder();

  @Inject
  IdentityServiceImpl(UserMapper userMapper,
                      IdentityEmailManager identityEmailManager) {
    this.userMapper = userMapper;
    this.identityEmailManager = identityEmailManager;
  }

  @Override
  @Transactional
  public UserModelMutationResult create(User user, String password) {

    if (userMapper.get(user.getUserName()) != null ||
            userMapper.getByEmail(user.getEmail()) != null) {
      return withError(ModelMutationError.USER_ALREADY_EXIST);
    }

    if(StringUtils.isBlank(password) || !PASSWORD_LENGTH_RANGE.contains(password.length())) {
      return withError(ModelMutationError.PASSWORD_LENGTH_VIOLATION);
    }

    user.setPasswordHash(encoder.encode(password));

    Set<ConstraintViolation<User>> violations = BEAN_VALIDATOR.validate(user,
            PrePersist.class, Default.class);
    if(!violations.isEmpty()) {
      return withError(violations);
    }
    userMapper.create(user);

    UUID challengeCode = UUID.randomUUID();
    userMapper.setChallengeCode(user.getKey(), challengeCode);

    //trigger email
    identityEmailManager.generateAndSendUserCreated(user, challengeCode);

    return UserModelMutationResult.onSuccess(user.getUserName(), user.getEmail());
  }

  @Override
  public UserModelMutationResult update(User user) {

    User currentUser = getByKey(user.getKey());
    if (currentUser != null) {

      //handle email change and user if the user want to change it is is not already
      //linked to another account
      if (!currentUser.getEmail().equalsIgnoreCase(user.getEmail())) {
        User currentUserWithEmail = userMapper.getByEmail(user.getEmail());
        if (currentUserWithEmail != null) {
          return UserModelMutationResult.withError(ModelMutationError.EMAIL_ALREADY_IN_USE);
        }
      }

      Set<ConstraintViolation<User>> violations = BEAN_VALIDATOR.validate(user,
              PostPersist.class, Default.class);
      if (!violations.isEmpty()) {
        return UserModelMutationResult.withError(violations);
      }
      userMapper.update(user);
    } else {
      //means the user doesn't exist
      return null;
    }
    return UserModelMutationResult.onSuccess(user.getUserName(), user.getEmail());
  }

  @Override
  public void delete(int userKey) {
    userMapper.delete(userKey);
  }

  @Override
  public User getByKey(int key) {
    return userMapper.getByKey(key);
  }

  @Override
  public User get(String username) {
    if (Strings.isNullOrEmpty(username)) {
      return null;
    }
    return userMapper.get(username);
  }

  @Override
  public User getByEmail(String email) {
    return userMapper.getByEmail(email);
  }

  @Override
  public PagingResponse<User> list(@Nullable Pageable pageable) {
    return search(null, pageable);
  }

  @Override
  public PagingResponse<User> search(@Nullable String query, @Nullable Pageable pageable) {
    return pagingResponse(pageable, userMapper.count(query), userMapper.search(query, pageable));
  }

  /**
   * Authenticate a user
   * @param username username or email address
   * @param password clear text password
   *
   * @return
   */
  @Override
  public User authenticate(String username, String password) {
    if (Strings.isNullOrEmpty(username) || password == null) {
      return null;
    }

    User u = getByIdentifier(username);
    if (u != null) {
      // use the settings which are the prefix in the existing password hash to encode the provided password
      // and verify that they result in the same
      String phash = encoder.encode(password, u.getPasswordHash());
      if (phash.equalsIgnoreCase(u.getPasswordHash())) {

        //ensure there is no pending challenge code, unless the user already logged in in the past.
        //If the user logged in in the past we assume the challengeCode is from a reset password and since we
        //can't be sure the user initiated the request himself we must allow to login
        if(userMapper.getChallengeCode(u.getKey()) == null || u.getLastLogin() != null) {
          return u;
        }
      }
    }
    return null;
  }

  @Override
  public User getByIdentifier(String identifier) {
    //this assumes username name can not contains @ (see User's getUserName())
    return StringUtils.contains(identifier, "@") ?
            getByEmail(identifier) :get(identifier);
  }

  @Override
  public User getBySession(String session) {
    if (Strings.isNullOrEmpty(session)) {
      return null;
    }
    return userMapper.getBySession(session);
  }

  @Override
  public void updateLastLogin(int userKey){
    userMapper.updateLastLogin(userKey);
  }


  @Override
  public boolean containsChallengeCode(int userKey) {
    return userMapper.getChallengeCode(userKey) != null;
  }

  @Override
  public boolean isChallengeCodeValid(int userKey, UUID challengeCode) {
    if (challengeCode != null) {
      UUID expectedChallengeCode = userMapper.getChallengeCode(userKey);
      if (challengeCode.equals(expectedChallengeCode)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean confirmChallengeCode(int userKey, UUID challengeCode) {
    if (challengeCode != null) {
      if(isChallengeCodeValid(userKey, challengeCode)){
        // remove challenge code since it has been confirmed
        userMapper.setChallengeCode(userKey, null);
        return true;
      }
    }
    return false;
  }

  @Override
  public void resetPassword(int userKey) {
    // ensure the user exists
    User user = userMapper.getByKey(userKey);
    if (userMapper.getByKey(userKey) != null) {
      //we do NOT terminate all session now (we can't guarantee it was initiated by the user himself)
      UUID challengeCode = UUID.randomUUID();
      userMapper.setChallengeCode(userKey, challengeCode);
      identityEmailManager.generateAndSendPasswordReset(user, challengeCode);
    }
  }

  @Override
  public UserModelMutationResult updatePassword(int userKey, String newPassword, UUID challengeCode) {
    if(confirmChallengeCode(userKey, challengeCode)){
      return updatePassword(userKey, newPassword);
    }
    return withSingleConstraintViolation(PropertyConstants.CHALLENGE_CODE_PROPERTY_NAME, PropertyConstants.CONSTRAINT_INCORRECT);
  }

  @Override
  public UserModelMutationResult updatePassword(int userKey, String newPassword) {
    User user = userMapper.getByKey(userKey);
    if(user != null){
      if(StringUtils.isBlank(newPassword) || !PASSWORD_LENGTH_RANGE.contains(newPassword.length())) {
        return withError(ModelMutationError.PASSWORD_LENGTH_VIOLATION);
      }
      user.setPasswordHash(encoder.encode(newPassword));
      userMapper.update(user);
    }
    else{
      return withSingleConstraintViolation("user", PropertyConstants.CONSTRAINT_UNKNOWN);
    }
    return UserModelMutationResult.onSuccess();
  }

  /**
   * Null safe builder to construct a paging response.
   *
   * @param page page to create response for, can be null
   */
  private static PagingResponse<User> pagingResponse(@Nullable Pageable page, long count, List<User> result) {
    return new PagingResponse<>(page == null ? new PagingRequest() : page, count, result);
  }

}
