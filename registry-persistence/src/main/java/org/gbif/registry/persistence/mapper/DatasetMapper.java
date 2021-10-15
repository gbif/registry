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
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Grid;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DatasetMapper extends BaseNetworkEntityMapper<Dataset> {

  /** Obtains a list of all the constituent datasets that are part of this parent dataset. */
  List<Dataset> listConstituents(
      @Param("parentKey") UUID parentKey, @Nullable @Param("page") Pageable page);

  /** Obtains a list of all the constituent datasets that are part of this network. */
  List<Dataset> listDatasetsInNetwork(
      @Param("networkKey") UUID networkKey, @Nullable @Param("page") Pageable page);

  /** Obtains a list of all the datasets published by the given organization. */
  List<Dataset> listDatasetsPublishedBy(
      @Param("organizationKey") UUID organizationKey, @Nullable @Param("page") Pageable page);

  /** Obtains a list of all the datasets hosted by, but not published by, the given organization. */
  List<Dataset> listDatasetsHostedBy(
      @Param("organizationKey") UUID organizationKey, @Nullable @Param("page") Pageable page);

  /**
   * Obtains a list of all the datasets published by an organization that is endorsed by the given
   * node.
   */
  List<Dataset> listDatasetsEndorsedBy(
      @Param("nodeKey") UUID nodeKey, @Nullable @Param("page") Pageable page);

  /**
   * Obtains a list of all the datasets filter optionally by a given country and optionally by a
   * type.
   */
  List<Dataset> listWithFilter(
      @Nullable @Param("country") Country country,
      @Nullable @Param("type") DatasetType type,
      @Nullable @Param("page") Pageable page);

  /**
   * Obtains a list of all datasets using the provided filter(s)
   *
   * @param from lower bound dataset modified date (inclusive)
   * @param to upper bound dataset modified date (exclusive)
   */
  List<Dataset> listWithFilter(
      @Nullable @Param("country") Country country,
      @Nullable @Param("type") DatasetType type,
      @Nullable @Param("installationKey") UUID installationKey,
      @Nullable @Param("dateFrom") Date from,
      @Nullable @Param("dateTo") Date to,
      @Nullable @Param("page") Pageable page);

  /** Counts all datasets from a DOI. This counts for dataset.doi and alternate identifiers. */
  long countByDOI(@Param("doi") String doi);

  /**
   * Get a Dataset list from a DOI, dataset.dois and alternate identifier are searched. Multiple
   * datasets could share the same DOI since this is not enforced.
   */
  List<Dataset> listByDOI(@Param("doi") String doi, @Nullable @Param("page") Pageable page);

  /** Count all datasets having all non null filters given. */
  int countWithFilter(
      @Nullable @Param("country") Country country, @Nullable @Param("type") DatasetType type);

  /** Count all datasets having all non null filters given. */
  int countWithFilter(
      @Nullable @Param("country") Country country,
      @Nullable @Param("type") DatasetType type,
      @Nullable @Param("installationKey") UUID installationKey,
      @Nullable @Param("dateFrom") Date from,
      @Nullable @Param("dateTo") Date to);

  /** Obtains a list of all the datasets hosted by the given installation. */
  List<Dataset> listDatasetsByInstallation(
      @Param("installationKey") UUID installationKey, @Nullable @Param("page") Pageable page);

  /** Count of datasets hosted by the given installation. */
  long countDatasetsByInstallation(@Param("installationKey") UUID installationKey);

  /** Count of datasets published by an organization that is endorsed by the given node. */
  long countDatasetsEndorsedBy(@Param("nodeKey") UUID nodeKey);

  long countDatasetsHostedBy(@Param("organizationKey") UUID organizationKey);

  long countDatasetsPublishedBy(@Param("organizationKey") UUID organizationKey);

  // sigh - int required by the model object, but the paging is long
  int countConstituents(@Param("key") UUID datasetKey);

  List<Dataset> deleted(@Nullable @Param("page") Pageable page);

  long countDeleted();

  List<Dataset> duplicates(@Nullable @Param("page") Pageable page);

  long countDuplicates();

  List<Dataset> subdatasets(@Nullable @Param("page") Pageable page);

  long countSubdatasets();

  List<Dataset> withNoEndpoint(@Nullable @Param("page") Pageable page);

  long countWithNoEndpoint();

  /**
   * Get the list of distinct countries of organizations that serves at least one dataset(not
   * flagged as deleted).
   *
   * @return The list of distinct countries of organizations that serves at least one dataset.
   */
  List<Country> listDistinctCountries(@Nullable @Param("page") Pageable page);

  /**
   * Get the list of installations that serves at least one dataset (not flagged as deleted). Note
   * that if the installation is flagged as 'deleted' but not the dataset, the dataset will be
   * included.
   *
   * @return The list of distinct installations that serves at least one dataset.
   */
  List<Installation> listDistinctInstallations(@Nullable @Param("page") Pageable page);

  List<Grid> listGrids(@Param("datasetKey") UUID datasetKey);
}
