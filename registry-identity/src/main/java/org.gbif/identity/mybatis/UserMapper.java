package org.gbif.identity.mybatis;

import org.gbif.api.model.common.User;

public interface UserMapper {
  void create(User user);
  User get(String userName);
  void delete(String userName);
  void update(User user);
  User getBySession(String session);
}
