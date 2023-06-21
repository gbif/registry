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
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.search.InstallationRequestSearchParams;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.model.registry.search.NetworkRequestSearchParams;
import org.gbif.api.model.registry.search.NodeRequestSearchParams;
import org.gbif.api.service.registry.NetworkService;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("network")
public interface NetworkClient extends NetworkEntityClient<Network>, NetworkService {

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/constituents",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Dataset> listConstituents(
      @PathVariable("key") UUID key, @SpringQueryMap Pageable pageable);

  @RequestMapping(method = RequestMethod.POST, value = "{key}/constituents/{datasetKey}")
  @Override
  void addConstituent(@PathVariable("key") UUID key, @PathVariable("datasetKey") UUID datasetKey);

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}/constituents/{datasetKey}")
  @Override
  void removeConstituent(
      @PathVariable("key") UUID key, @PathVariable("datasetKey") UUID datasetKey);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "suggest",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  List<KeyTitleResult> suggest(@RequestParam(value = "q", required = false) String q);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/organization",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Organization> publishingOrganizations(
      @PathVariable("key") UUID key, @SpringQueryMap Pageable page);

  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Network> list(@SpringQueryMap NetworkRequestSearchParams searchParams);
}
