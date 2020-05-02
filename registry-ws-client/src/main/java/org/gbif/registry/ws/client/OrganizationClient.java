/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(name = "organizationClient", url = "${registry.ws.url}", primary = false)
@RequestMapping("organization")
public interface OrganizationClient extends OrganizationService {

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
  PagingResponse<Organization> listDeleted(@SpringQueryMap Pageable pageable);

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

  @Override
  default boolean confirmEndorsement(UUID organizationKey, UUID confirmationKey) {
    throw new UnsupportedOperationException("OrganizationClient confirm endorsement not supported");
  }
}
