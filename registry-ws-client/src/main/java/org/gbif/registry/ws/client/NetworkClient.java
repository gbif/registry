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
package org.gbif.registry.ws.client;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.model.registry.search.NetworkRequestSearchParams;
import org.gbif.api.service.registry.NetworkService;

import java.util.List;
import java.util.UUID;

/**
 * Feign client for network endpoints.
 *
 * Base URL must include the "network" prefix when creating the Feign client.
 */
@Headers("Accept: application/json")
public interface NetworkClient extends NetworkEntityClient<Network>, NetworkService {

  @RequestLine("GET network/{key}/constituents")
  PagingResponse<Dataset> listConstituents(@Param("key") UUID key, Pageable pageable);

  @RequestLine("POST network/{key}/constituents/{datasetKey}")
  void addConstituent(@Param("key") UUID key, @Param("datasetKey") UUID datasetKey);

  @RequestLine("DELETE network/{key}/constituents/{datasetKey}")
  void removeConstituent(@Param("key") UUID key, @Param("datasetKey") UUID datasetKey);

  @RequestLine("GET network/suggest?q={q}")
  List<KeyTitleResult> suggest(@Param("q") String q);

  @RequestLine("GET network/{key}/organization")
  PagingResponse<Organization> publishingOrganizations(@Param("key") UUID key, Pageable page);

  @RequestLine("GET network")
  PagingResponse<Network> list(NetworkRequestSearchParams searchParams);
}
