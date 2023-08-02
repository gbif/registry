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
import org.gbif.api.model.registry.EndorsementStatus;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.registry.domain.ws.LegacyOrganizationBriefResponse;
import org.gbif.registry.persistence.ChallengeCodeSupportMapper;
import org.gbif.registry.persistence.mapper.params.OrganizationListParams;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Qualifier("organizationChallengeCodeSupportMapper")
@Repository
public interface OrganizationMapper
    extends BaseNetworkEntityMapper<Organization>, ChallengeCodeSupportMapper<UUID> {

  List<Organization> list(@Param("params") OrganizationListParams params);

  long count(@Param("params") OrganizationListParams params);

  /** Endorse organization by key. */
  void endorse(@Param("key") UUID key);

  /** Revoke organization endorsement by key. */
  void revokeEndorsement(@Param("key") UUID key);

  /** Change endorsement status. */
  void changeEndorsementStatus(
      @Param("key") UUID organizationKey, @Param("status") EndorsementStatus status);

  /**
   * @return The count of organizations that publish no datasets
   */
  long countNonPublishing();

  /**
   * @return The organizations that publish no datasets
   */
  // TODO: merge into list?
  List<Organization> nonPublishing(@Param("page") Pageable page);

  /**
   * @return The organizations that have an installation of the given type, optionally filtered to
   *     be georeferenced only
   */
  List<Organization> hostingInstallationsOf(
      @Param("type") InstallationType type,
      @Nullable @Param("georeferenced") Boolean georeferencedOnly);

  /**
   * @return a count of endorsed, non deleted organizations that publish data.
   */
  int countPublishing();

  /**
   * @return The list of organizations, with only their key and title populated.
   */
  List<LegacyOrganizationBriefResponse> listLegacyOrganizationsBrief();

  /** A simple suggest by title service. */
  List<KeyTitleResult> suggest(@Nullable @Param("q") String q);

  Organization getLightweight(@Param("key") UUID key);
}
