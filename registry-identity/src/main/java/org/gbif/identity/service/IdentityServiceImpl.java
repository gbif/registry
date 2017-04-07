package org.gbif.identity.service;

import org.gbif.api.model.common.User;
import org.gbif.api.model.common.UserCreation;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.identity.email.IdentityEmailManager;
import org.gbif.identity.model.ModelError;
import org.gbif.identity.model.Session;
import org.gbif.identity.model.UserCreationResult;
import org.gbif.identity.mybatis.SessionMapper;
import org.gbif.identity.mybatis.UserMapper;
import org.gbif.identity.util.PasswordEncoder;
import org.gbif.identity.util.SessionTokens;

import java.util.List;
import java.util.Optional;
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
import org.apache.commons.lang3.StringUtils;


/**
 * Main implementation of {@link IdentityService} on top of mybatis.
 */
class IdentityServiceImpl implements IdentityService {

  private final UserMapper userMapper;
  private final SessionMapper sessionMapper;
  private final IdentityEmailManager identityEmailManager;

  private final Validator BEAN_VALIDATOR =
          Validation.byProvider(ApacheValidationProvider.class)
                  .configure()
                  .buildValidatorFactory()
                  .getValidator();
  private final PasswordEncoder encoder = new PasswordEncoder();

  @Inject
  IdentityServiceImpl(UserMapper userMapper, SessionMapper sessionMapper,
                             IdentityEmailManager identityEmailManager) {
    this.userMapper = userMapper;
    this.sessionMapper = sessionMapper;
    this.identityEmailManager = identityEmailManager;
  }

  @Override
  public UserCreationResult create(UserCreation user) {
    if (userMapper.get(user.getUserName()) != null ||
            userMapper.getByEmail(user.getEmail()) != null) {
      return UserCreationResult.withError(ModelError.USER_ALREADY_EXIST);
    }

    Set<ConstraintViolation<UserCreation>> violations = BEAN_VALIDATOR.validate(user,
            PrePersist.class, Default.class);
    if(!violations.isEmpty()) {
      return UserCreationResult.withError(violations);
    }

    String passwordHash = encoder.encode(user.getPassword());
    User newUser = UserCreation.toUser(user);
    newUser.getRoles().add(UserRole.USER);
    newUser.setPasswordHash(passwordHash);
    userMapper.create(newUser);

    UUID challengeCode = UUID.randomUUID();
    userMapper.setChallengeCode(newUser.getKey(), challengeCode);

    //trigger email
    identityEmailManager.generateAndSendUserCreated(newUser, challengeCode);

    return UserCreationResult.onSuccess(newUser.getUserName(), newUser.getEmail());
  }

  @Override
  public void update(User user) {
    Optional.ofNullable(user.getUserName())
            .map(this::get)
            .ifPresent(
                    currentUser -> {
                      //control which field it is possible to update for the user himself
                      currentUser.setFirstName(user.getFirstName());
                      currentUser.setLastName(user.getLastName());
                      currentUser.setSettings(user.getSettings());
                      userMapper.update(currentUser);
                    }
            );
  }


  @Override
  public void delete(String username) {
    userMapper.delete(username);
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
        //ensure there is no pending challenge code
        if(userMapper.getChallengeCode(u.getKey()) == null) {
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

  public Session createSession(String username) {
    Session session = new Session(username, SessionTokens.newSessionToken(username));
    sessionMapper.create(session);
    return session;
  }

  public List<Session> listSessions(String username) {
    return sessionMapper.list(username);
  }

  public void terminateSession(String session) {
    sessionMapper.delete(session);
  }

  public void terminateAllSessions(String username) {
    sessionMapper.deleteAll(username);
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
  public boolean updatePassword(int userKey, String newPassword, UUID challengeCode) {
    if(confirmChallengeCode(userKey, challengeCode)){
      User user = userMapper.getByKey(userKey);
      user.setPasswordHash(encoder.encode(newPassword));
      userMapper.update(user);
      return true;
    }
    return false;
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
