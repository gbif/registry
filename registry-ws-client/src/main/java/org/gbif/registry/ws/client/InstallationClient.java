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
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.metasync.MetasyncHistory;
import org.gbif.api.model.registry.search.InstallationRequestSearchParams;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.MetasyncHistoryService;
import org.gbif.api.vocabulary.InstallationType;

import java.util.List;
import java.util.UUID;

/**
 * Feign client for installation and metasync endpoints.
 *
 * Base path must be included in the target URL when building the client.
 */
@Headers("Accept: application/json")
public interface InstallationClient extends
  NetworkEntityClient<Installation>, InstallationService, MetasyncHistoryService {

  @RequestLine("GET installation/{key}/dataset")
  PagingResponse<Dataset> getHostedDatasets(@Param("key") UUID key, Pageable pageable);

  @RequestLine("GET installation/deleted")
  PagingResponse<Installation> listDeleted(InstallationRequestSearchParams searchParams);

  @RequestLine("GET installation/nonPublishing")
  PagingResponse<Installation> listNonPublishing(Pageable pageable);

  @RequestLine("GET installation/suggest?q={q}")
  List<KeyTitleResult> suggest(@Param("q") String q);

  @RequestLine("GET installation?type={type}")
  PagingResponse<Installation> listByType(@Param("type") InstallationType type, Pageable pageable);

  @RequestLine("POST installation/metasync")
  @Headers("Content-Type: application/json")
  void createMetasync(MetasyncHistory metasyncHistory);

  @RequestLine("GET installation/metasync")
  PagingResponse<MetasyncHistory> listMetasync(Pageable pageable);

  @RequestLine("GET installation/{installationKey}/metasync")
  PagingResponse<MetasyncHistory> listMetasync(@Param("installationKey") UUID installationKey, Pageable pageable);

  @RequestLine("GET installation")
  PagingResponse<Installation> list(InstallationRequestSearchParams searchParams);
}
