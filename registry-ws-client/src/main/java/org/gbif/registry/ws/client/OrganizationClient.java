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
import feign.QueryMap;
import feign.RequestLine;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.model.registry.search.OrganizationRequestSearchParams;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.geojson.FeatureCollection;

import java.util.List;
import java.util.UUID;

@Headers("Accept: application/json")
public interface OrganizationClient extends NetworkEntityClient<Organization>, OrganizationService {

  @RequestLine("GET /{key}/hostedDataset")
  PagingResponse<Dataset> hostedDatasets(@Param("key") UUID key, @QueryMap Pageable pageable);

  @RequestLine("GET /{key}/publishedDataset")
  PagingResponse<Dataset> publishedDatasets(@Param("key") UUID key, @QueryMap Pageable page);

  @RequestLine("GET /{key}/installation")
  PagingResponse<Installation> installations(@Param("key") UUID key, @QueryMap Pageable pageable);

  @RequestLine("GET /?country={country}")
  PagingResponse<Organization> listByCountry(@Param("country") Country country, @QueryMap Pageable pageable);

  @RequestLine("GET /deleted")
  PagingResponse<Organization> listDeleted(@QueryMap OrganizationRequestSearchParams searchParams);

  @RequestLine("GET /pending")
  PagingResponse<Organization> listPendingEndorsement(@QueryMap Pageable pageable);

  @RequestLine("GET /nonPublishing")
  PagingResponse<Organization> listNonPublishing(@QueryMap Pageable pageable);

  @RequestLine("GET /suggest?q={q}")
  List<KeyTitleResult> suggest(@Param("q") String q);

  @RequestLine("POST /{key}/endorsement/{confirmationKey}")
  boolean confirmEndorsement(@Param("key") UUID organizationKey, @Param("confirmationKey") UUID confirmationKey);

  @RequestLine("PUT /{key}/endorsement")
  void confirmEndorsementEndpoint(@Param("key") UUID organizationKey);

  @RequestLine("DELETE /{key}/endorsement")
  void revokeEndorsementEndpoint(@Param("key") UUID organizationKey);

  @RequestLine("GET /")
  PagingResponse<Organization> list(@QueryMap OrganizationRequestSearchParams searchParams);

  @RequestLine("GET /geojson")
  FeatureCollection listGeoJson(@QueryMap OrganizationRequestSearchParams searchParams);
}

