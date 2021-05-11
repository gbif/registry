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
package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.persistence.ChallengeCodeSupportMapper;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Qualifier("userChallengeCodeSupportMapper")
@Repository
public interface UserMapper extends ChallengeCodeSupportMapper<Integer> {

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

  void deleteByKey(int key);

  void delete(GbifUser user);

  void update(GbifUser user);

  List<GbifUser> search(
    @Nullable @Param("query") String query, @Nullable @Param("roles") Set<UserRole> roles, @Nullable @Param("editorRightsOn") Set<UUID> editorRightsOn, @Nullable @Param("page") Pageable page);

  int count(@Nullable @Param("query") String query, @Nullable @Param("roles") Set<UserRole> roles, @Nullable @Param("editorRightsOn") Set<UUID> editorRightsOn);

  /*
   * Editor rights
   */
  List<UUID> listEditorRights(@Param("userName") String userName);

  void addEditorRight(@Param("userName") String userName, @Param("key") UUID key);

  void deleteEditorRight(@Param("userName") String userName, @Param("key") UUID key);
}
