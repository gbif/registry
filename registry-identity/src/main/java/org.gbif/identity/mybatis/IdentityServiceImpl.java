package org.gbif.identity.mybatis;

import org.gbif.api.model.common.User;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.common.IdentityService;
import org.gbif.identity.model.Session;
import org.gbif.identity.util.PasswordEncoder;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdentityServiceImpl implements IdentityService {

  private static final Logger LOG = LoggerFactory.getLogger(IdentityServiceImpl.class);

  private final UserMapper userMapper;
  private final SessionMapper sessionMapper;
  private final PasswordEncoder encoder = new PasswordEncoder();

  @Inject
  public IdentityServiceImpl(UserMapper userMapper, SessionMapper sessionMapper) {
    this.userMapper = userMapper;
    this.sessionMapper = sessionMapper;
  }

  @Override
  public String create(User user) {
    userMapper.create(user);
    return user.getUserName();
  }

  @Override
  public void delete(String username) {
    userMapper.delete(username);
  }

  @Override
  public User get(String username) {
    if (Strings.isNullOrEmpty(username)) {
      return null;
    }
    return userMapper.get(username);
  }

  @Override
  public PagingResponse<User> list(@Nullable Pageable pageable) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void update(User user) {
    userMapper.update(user);
  }

  @Override
  public User authenticate(String username, String password) {
    if (Strings.isNullOrEmpty(username) || password == null) {
      return null;
    }

    User u = get(username);
    if (u != null) {
      // use the settings which are the prefix in the existing password hash to encode the provided password
      // and verify that they result in the same
      String phash = encoder.encode(password, u.getPasswordHash());
      if (phash.equalsIgnoreCase(u.getPasswordHash())) {
        return u;
      }
    }

    return null;
  }

  @Override
  public User getBySession(String session) {
    if (Strings.isNullOrEmpty(session)) {
      return null;
    }
    return userMapper.getBySession(session);
  }

  // TODO... where should this go?
  public Session authenticate(String username, String password, String host) {
    User u = authenticate(username, password);
    if (u != null) {
      Session session = new Session(username, host, UUID.randomUUID().toString());
      sessionMapper.create(session);
      return session; // TODO
    }
    return null;
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
}
