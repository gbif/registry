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
import feign.Body;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.*;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.model.registry.search.NodeRequestSearchParams;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.vocabulary.Country;

import java.util.List;
import java.util.UUID;

@Headers("Accept: application/json")
public interface NodeClient extends NetworkEntityClient<Node>, NodeService {

  @RequestLine("GET {key}/organization?page={page}&limit={limit}")
  PagingResponse<Organization> endorsedOrganizations(
    @Param("key") UUID key,
    @Param("page") int page,
    @Param("limit") int limit);

  @RequestLine("GET pendingEndorsement?page={page}&limit={limit}")
  PagingResponse<Organization> pendingEndorsements(
    @Param("page") int page,
    @Param("limit") int limit);

  @RequestLine("GET {key}/pendingEndorsement?page={page}&limit={limit}")
  PagingResponse<Organization> pendingEndorsements(
    @Param("key") UUID key,
    @Param("page") int page,
    @Param("limit") int limit);

  @RequestLine("GET {key}/installation?page={page}&limit={limit}")
  PagingResponse<Installation> installations(
    @Param("key") UUID key,
    @Param("page") int page,
    @Param("limit") int limit);

  default Node getByCountry(Country country) {
    return getByCountry(country.getIso2LetterCode());
  }

  @RequestLine("GET country/{isoCode}")
  Node getByCountry(@Param("isoCode") String isoCode);

  @RequestLine("GET country")
  List<Country> listNodeCountries();

  @RequestLine("GET activeCountries")
  List<Country> listActiveCountries();

  @RequestLine("GET {key}/dataset?page={page}&limit={limit}")
  PagingResponse<Dataset> endorsedDatasets(
    @Param("key") UUID key,
    @Param("page") int page,
    @Param("limit") int limit);

  @RequestLine("GET suggest?q={query}")
  List<KeyTitleResult> suggest(@Param("query") String query);

  @RequestLine("GET")
  PagingResponse<Node> list(NodeRequestSearchParams searchParams);
}
