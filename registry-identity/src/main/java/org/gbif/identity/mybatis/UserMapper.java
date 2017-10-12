package org.gbif.identity.mybatis;

import org.gbif.api.model.common.GbifUser;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.registry.surety.persistence.ChallengeCodeSupportMapper;

import java.util.List;
import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

public interface UserMapper extends ChallengeCodeSupportMapper<Integer> {
  void create(GbifUser user);

  GbifUser get(String userName);
  GbifUser getByKey(int key);
  GbifUser getByEmail(String email);
  GbifUser getBySystemSetting(@Param("key")String key, @Param("value")String value);

  /**
   * Update the lastLogin date to "now()".
   * @param key
   */
  void updateLastLogin(@Param("key")int key);

  void delete(int key);
  void update(GbifUser user);
  List<GbifUser> search(@Nullable @Param("query") String query, @Nullable @Param("page") Pageable page);
  int count(@Nullable @Param("query") String query);
}
