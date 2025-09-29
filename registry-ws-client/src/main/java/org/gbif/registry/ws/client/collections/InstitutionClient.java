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

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.InstitutionImportParams;
import org.gbif.api.model.collections.latimercore.OrganisationalUnit;
import org.gbif.api.model.collections.merge.ConvertToCollectionParams;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.collections.suggestions.InstitutionChangeSuggestion;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;

import java.util.List;
import java.util.UUID;

import org.geojson.FeatureCollection;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RequestMapping("grscicoll/institution")
public interface InstitutionClient
    extends BaseCollectionEntityClient<Institution, InstitutionChangeSuggestion> {

  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<Institution> list(@SpringQueryMap InstitutionSearchRequest searchRequest);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "latimerCore",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<OrganisationalUnit> listAsLatimerCore(
      @SpringQueryMap InstitutionSearchRequest searchRequest);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "geojson",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  FeatureCollection listAsGeoJson(@SpringQueryMap InstitutionSearchRequest searchRequest);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "latimerCore/{key}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  OrganisationalUnit getAsLatimerCore(@PathVariable("key") UUID key);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "latimerCore",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  UUID createFromLatimerCore(@RequestBody OrganisationalUnit organisationalUnit);

  @RequestMapping(
      method = RequestMethod.PUT,
      value = "latimerCore/{key}",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  void updateFromLatimerCore(
      @PathVariable("key") UUID key, @RequestBody OrganisationalUnit organisationalUnit);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "deleted",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<Institution> listDeleted(@SpringQueryMap InstitutionSearchRequest searchRequest);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "suggest",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  List<KeyCodeNameResult> suggest(@RequestParam(value = "q", required = false) String q);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/convertToCollection",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  UUID convertToCollection(
      @PathVariable("key") UUID entityKey, @RequestBody ConvertToCollectionParams params);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "import",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  UUID createFromOrganization(@RequestBody InstitutionImportParams importParams);
}
