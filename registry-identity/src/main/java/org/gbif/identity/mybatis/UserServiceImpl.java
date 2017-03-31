package org.gbif.identity.mybatis;

import org.gbif.api.model.common.User;
import org.gbif.api.service.common.UserService;
import org.gbif.identity.util.PasswordEncoder;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserServiceImpl implements UserService {

  private static final Logger LOG = LoggerFactory.getLogger(UserServiceImpl.class);

  private final UserMapper mapper;
  private final PasswordEncoder encoder = new PasswordEncoder();

  @Inject
  public UserServiceImpl(UserMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public User get(String username) {
    if (Strings.isNullOrEmpty(username)) {
      return null;
    }

    User u = mapper.get(username);
    // make sure we found a user with that name
    if (u == null) {
      LOG.debug("Cannot find user " + username);
    }
    return u;
  }

  @Override
  public User authenticate(String username, String password) {
    if (Strings.isNullOrEmpty(username) || password == null) {
      return null;
    }

    User u = get(username);
    if (u != null) {
      // build password hash stored in drupal, using the existing hash which contains also encoding settings
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

    User u = mapper.getBySession(session);
    // make sure we found a user with that name
    if (u == null) {
      LOG.debug("Cannot find user session " + session);
    }

    return u;
  }
}
