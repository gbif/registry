package org.gbif.identity.mybatis;

import org.gbif.api.model.common.User;
import org.gbif.api.model.common.paging.Pageable;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

public interface UserMapper {
  void create(User user);

  User get(String userName);
  User getByKey(int key);
  User getByEmail(String email);

  void setChallengeCode(@Param("key")int key, @Nullable @Param("challengeCode") UUID challengeCode);
  UUID getChallengeCode(int key);

  /**
   * Update the lastLogin date to "now()".
   * @param key
   */
  void updateLastLogin(@Param("key")int key);

  void delete(int key);
  void update(User user);
  User getBySession(String session);
  List<User> search(@Nullable @Param("query") String query, @Nullable @Param("page") Pageable page);
  int count(@Nullable @Param("query") String query);
}
