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
package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.vocabulary.InstallationType;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InstallationMapper extends BaseNetworkEntityMapper<Installation> {

  long countInstallationsEndorsedBy(@Param("nodeKey") UUID nodeKey);

  List<Installation> listInstallationsEndorsedBy(
      @Param("nodeKey") UUID nodeKey, @Nullable @Param("page") Pageable page);

  long countInstallationsByOrganization(@Param("organizationKey") UUID organizationKey);

  List<Installation> listInstallationsByOrganization(
      @Param("organizationKey") UUID organizationKey, @Nullable @Param("page") Pageable page);

  List<Installation> deleted(@Nullable @Param("page") Pageable page);

  long countDeleted();

  long countNonPublishing();

  List<Installation> nonPublishing(@Nullable @Param("page") Pageable page);

  /** A simple suggest by title service. */
  List<KeyTitleResult> suggest(@Nullable @Param("q") String q);

  /** Count all installations having all non null filters given. */
  int countWithFilter(@Nullable @Param("type") InstallationType type);

  /** Obtains a list of all installations filtered optionally by a type. */
  List<Installation> listWithFilter(
      @Nullable @Param("type") InstallationType type, @Nullable @Param("page") Pageable page);
}
