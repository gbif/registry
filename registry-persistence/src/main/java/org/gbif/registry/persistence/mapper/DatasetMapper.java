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
import org.gbif.registry.persistence.mapper.params.DatasetListParams;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DatasetMapper extends BaseNetworkEntityMapper<Dataset> {

  List<Dataset> list(@Param("params") DatasetListParams params);

  long count(@Param("params") DatasetListParams params);

  // TODO: merge the others wiht the list?
  /** Obtains a list of all the datasets hosted by, but not published by, the given organization. */
  List<Dataset> listDatasetsHostedBy(
      @Param("organizationKey") UUID organizationKey, @Nullable @Param("page") Pageable page);

  /**
   * Obtains a list of all the datasets published by an organization that is endorsed by the given
   * node.
   */
  List<Dataset> listDatasetsEndorsedBy(
      @Param("nodeKey") UUID nodeKey, @Nullable @Param("page") Pageable page);

  /** Count of datasets published by an organization that is endorsed by the given node. */
  long countDatasetsEndorsedBy(@Param("nodeKey") UUID nodeKey);

  long countDatasetsHostedBy(@Param("organizationKey") UUID organizationKey);

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
