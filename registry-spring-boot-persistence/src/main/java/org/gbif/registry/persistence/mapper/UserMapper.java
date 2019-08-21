package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.registry.persistence.mapper.surety.ChallengeCodeSupportMapper;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserMapper extends ChallengeCodeSupportMapper<Integer> {

  // TODO: 2019-06-26 OrganizationMapper, UserMapper
  Integer getChallengeCodeKey(@Param("key") Integer key);

  boolean updateChallengeCodeKey(@Param("key") Integer key, @Param("challengeCodeKey") Integer challengeCodeKey);

  void create(GbifUser user);

  GbifUser get(String userName);

  GbifUser getByKey(int key);

  GbifUser getByEmail(String email);

  GbifUser getBySystemSetting(@Param("key") String key, @Param("value") String value);

  /**
   * Update the lastLogin date to "now()".
   *
   * @param key
   */
  void updateLastLogin(@Param("key") int key);

  void delete(int key);

  void update(GbifUser user);

  List<GbifUser> search(@Nullable @Param("query") String query, @Nullable @Param("page") Pageable page);

  int count(@Nullable @Param("query") String query);

  /*
   * Editor rights
   */
  List<UUID> listEditorRights(@Param("userName") String userName);

  void addEditorRight(@Param("userName") String userName, @Param("key") UUID key);

  void deleteEditorRight(@Param("userName") String userName, @Param("key") UUID key);
}
