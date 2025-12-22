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
package org.gbif.registry.ws.client.collections;
import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.InstitutionImportParams;
import org.gbif.api.model.collections.latimercore.OrganisationalUnit;
import org.gbif.api.model.collections.merge.ConvertToCollectionParams;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.collections.suggestions.InstitutionChangeSuggestion;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.geojson.FeatureCollection;

import java.util.List;
import java.util.UUID;

public interface InstitutionClient extends BaseCollectionEntityClient<Institution, InstitutionChangeSuggestion> {

  @RequestLine("GET /grscicoll/institution")
  @Headers("Accept: application/json")
  PagingResponse<Institution> list(@QueryMap InstitutionSearchRequest searchRequest);

  @RequestLine("GET /grscicoll/institution/latimerCore")
  @Headers("Accept: application/json")
  PagingResponse<OrganisationalUnit> listAsLatimerCore(@QueryMap InstitutionSearchRequest searchRequest);

  @RequestLine("GET /grscicoll/institution/geojson")
  @Headers("Accept: application/json")
  FeatureCollection listAsGeoJson(@QueryMap InstitutionSearchRequest searchRequest);

  @RequestLine("GET /grscicoll/institution/latimerCore/{key}")
  @Headers("Accept: application/json")
  OrganisationalUnit getAsLatimerCore(@Param("key") UUID key);

  @RequestLine("POST /grscicoll/institution/latimerCore")
  @Headers("Content-Type: application/json")
  UUID createFromLatimerCore(OrganisationalUnit organisationalUnit);

  @RequestLine("PUT /grscicoll/institution/latimerCore/{key}")
  @Headers("Content-Type: application/json")
  void updateFromLatimerCore(@Param("key") UUID key, OrganisationalUnit organisationalUnit);

  @RequestLine("GET /grscicoll/institution/deleted")
  @Headers("Accept: application/json")
  PagingResponse<Institution> listDeleted(@QueryMap InstitutionSearchRequest searchRequest);

  @RequestLine("GET /grscicoll/institution/suggest?q={q}")
  @Headers("Accept: application/json")
  List<KeyCodeNameResult> suggest(@Param("q") String q);

  @RequestLine("POST /grscicoll/institution/{key}/convertToCollection")
  @Headers({"Content-Type: application/json", "Accept: application/json"})
  UUID convertToCollection(@Param("key") UUID entityKey, ConvertToCollectionParams params);

  @RequestLine("POST /grscicoll/institution/import")
  @Headers({"Content-Type: application/json", "Accept: application/json"})
  UUID createFromOrganization(InstitutionImportParams importParams);
}
