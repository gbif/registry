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

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.registry.domain.ws.LegacyOrganizationBriefResponse;
import org.gbif.registry.persistence.ChallengeCodeSupportMapper;

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

  /**
   * At higher levels this appears on the NodeService, but it makes a cleaner MyBatis implementation
   * on this mapper.
   */
  List<Organization> organizationsEndorsedBy(
      @Param("nodeKey") UUID nodeKey, @Nullable @Param("page") Pageable page);

  /**
   * Endorse organization by key.
   */
  void endorse(@Param("key") UUID key);

  /**
   * At higher levels this appears on the NodeService, but it makes a cleaner MyBatis implementation
   * on this mapper.
   */
  List<Organization> pendingEndorsements(
      @Nullable @Param("nodeKey") UUID nodeKey, @Nullable @Param("page") Pageable page);

  /**
   * At higher levels this appears on the OrganizationService. Selects all organizations by the
   * country of their address.
   */
  List<Organization> organizationsByCountry(
      @Param("country") Country country, @Nullable @Param("page") Pageable page);

  /** @return The count of all organizations with approved endorsements for the node */
  long countOrganizationsEndorsedBy(@Param("nodeKey") UUID nodeKey);

  /**
   * @return The count of all organizations with a pending endorsement approval, optionally limited
   *     by the node if supplied
   */
  long countPendingEndorsements(@Nullable @Param("nodeKey") UUID nodeKey);

  /** @return The count of all organizations for the given country */
  long countOrganizationsByCountry(@Param("country") Country country);

  /** @return The count of organizations marked as deleted */
  long countDeleted();

  /** @return The organizations marked as deleted */
  List<Organization> deleted(@Param("page") Pageable page);

  /** @return The count of organizations that publish no datasets */
  long countNonPublishing();

  /** @return The organizations that publish no datasets */
  List<Organization> nonPublishing(@Param("page") Pageable page);

  /**
   * @return The organizations that have an installation of the given type, optionally filtered to
   *     be georeferenced only
   */
  List<Organization> hostingInstallationsOf(
      @Param("type") InstallationType type,
      @Nullable @Param("georeferenced") Boolean georeferencedOnly);

  /** @return a count of endorsed, non deleted organizations that publish data. */
  int countPublishing();

  /** @return The list of organizations, with only their key and title populated. */
  List<LegacyOrganizationBriefResponse> listLegacyOrganizationsBrief();

  /** A simple suggest by title service. */
  List<KeyTitleResult> suggest(@Nullable @Param("q") String q);

  /** Overloaded search to allow it to scope the search by country. */
  List<Organization> search(
      @Nullable @Param("query") String query,
      @Param("country") Country country,
      @Param("isEndorsed") Boolean isEndorsed,
      @Nullable @Param("page") Pageable page);

  /** Overloaded count to allow a search scoped by country. */
  int count(
      @Nullable @Param("query") String query,
      @Param("country") Country country,
      @Param("isEndorsed") Boolean isEndorsed);
}
