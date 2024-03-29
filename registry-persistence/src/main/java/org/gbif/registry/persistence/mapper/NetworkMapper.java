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

import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.registry.domain.ws.IptNetworkBriefResponse;
import org.gbif.registry.persistence.mapper.params.NetworkListParams;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NetworkMapper extends BaseNetworkEntityMapper<Network> {

  List<Network> list(@Param("params") NetworkListParams params);

  long count(@Param("params") NetworkListParams params);

  boolean constituentExists(
      @Param("networkKey") UUID networkKey, @Param("datasetKey") UUID datasetKey);

  void addDatasetConstituent(
      @Param("networkKey") UUID networkKey, @Param("datasetKey") UUID datasetKey);

  void deleteDatasetConstituent(
      @Param("networkKey") UUID networkKey, @Param("datasetKey") UUID datasetKey);

  /** A simple suggest by title service. */
  List<KeyTitleResult> suggest(@Nullable @Param("q") String q);

  /**
   * @return The list of networks, with only their key and title populated.
   */
  List<IptNetworkBriefResponse> listNetworksBrief();
}
