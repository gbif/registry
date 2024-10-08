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

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.model.registry.search.OrganizationRequestSearchParams;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;

import java.util.List;
import java.util.UUID;

import org.geojson.FeatureCollection;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("organization")
public interface OrganizationClient extends NetworkEntityClient<Organization>, OrganizationService {

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/hostedDataset",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Dataset> hostedDatasets(
      @PathVariable("key") UUID key, @SpringQueryMap Pageable pageable);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/publishedDataset",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Dataset> publishedDatasets(
      @PathVariable("key") UUID key, @SpringQueryMap Pageable page);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/installation",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Installation> installations(
      @PathVariable("key") UUID key, @SpringQueryMap Pageable pageable);

  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Organization> listByCountry(
      @RequestParam("country") Country country, @SpringQueryMap Pageable pageable);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "deleted",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Organization> listDeleted(@SpringQueryMap OrganizationRequestSearchParams searchParams);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "pending",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Organization> listPendingEndorsement(@SpringQueryMap Pageable pageable);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "nonPublishing",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Organization> listNonPublishing(@SpringQueryMap Pageable pageable);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "suggest",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  List<KeyTitleResult> suggest(@RequestParam(value = "q", required = false) String q);

  @RequestMapping(method = RequestMethod.POST, value = "{key}/endorsement/{confirmationKey}")
  @ResponseBody
  @Override
  boolean confirmEndorsement(
      @PathVariable("key") UUID organizationKey,
      @PathVariable("confirmationKey") UUID confirmationKey);

  @RequestMapping(method = RequestMethod.PUT, value = "{key}/endorsement")
  @ResponseBody
  ResponseEntity<Void> confirmEndorsementEndpoint(@PathVariable("key") UUID organizationKey);

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}/endorsement")
  @ResponseBody
  ResponseEntity<Void> revokeEndorsementEndpoint(@PathVariable("key") UUID organizationKey);

  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Organization> list(@SpringQueryMap OrganizationRequestSearchParams searchParams);

  @GetMapping(value = "geojson", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  FeatureCollection listGeoJson(@SpringQueryMap OrganizationRequestSearchParams searchParams);
}
