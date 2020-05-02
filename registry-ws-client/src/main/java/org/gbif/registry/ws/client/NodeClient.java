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
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.service.registry.NodeService;
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

@FeignClient(name = "nodeClient", url = "${registry.ws.url}", primary = false)
@RequestMapping("node")
public interface NodeClient extends NetworkEntityClient<Node>, NodeService {

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/organization",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Organization> endorsedOrganizations(
      @PathVariable("key") UUID key, @SpringQueryMap Pageable pageable);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "pendingEndorsement",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Organization> pendingEndorsements(@SpringQueryMap Pageable pageable);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/pendingEndorsement",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Organization> pendingEndorsements(
      @PathVariable("key") UUID key, @SpringQueryMap Pageable pageable);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/installation",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Installation> installations(
      @PathVariable("key") UUID key, @SpringQueryMap Pageable pageable);

  @Override
  default Node getByCountry(Country country) {
    return getByCountry(country.getIso2LetterCode());
  }

  @RequestMapping(
      method = RequestMethod.GET,
      value = "country/{key}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  Node getByCountry(@PathVariable("key") String isoCode);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "country",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  List<Country> listNodeCountries();

  @RequestMapping(
      method = RequestMethod.GET,
      value = "activeCountries",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  List<Country> listActiveCountries();

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/dataset",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Dataset> endorsedDatasets(
      @PathVariable("key") UUID key, @SpringQueryMap Pageable pageable);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "suggest",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  List<KeyTitleResult> suggest(@RequestParam(value = "q", required = false) String q);
}
