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
import org.gbif.api.model.registry.metasync.MetasyncHistory;
import org.gbif.api.model.registry.search.InstallationRequestSearchParams;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.MetasyncHistoryService;
import org.gbif.api.vocabulary.InstallationType;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("installation")
public interface InstallationClient
    extends NetworkEntityClient<Installation>, InstallationService, MetasyncHistoryService {

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/dataset",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Dataset> getHostedDatasets(
      @PathVariable("key") UUID key, @SpringQueryMap Pageable pageable);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "deleted",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Installation> listDeleted(@SpringQueryMap InstallationRequestSearchParams searchParams);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "nonPublishing",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Installation> listNonPublishing(@SpringQueryMap Pageable pageable);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "suggest",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  List<KeyTitleResult> suggest(@RequestParam(value = "q", required = false) String q);

  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Installation> listByType(
      @RequestParam(value = "type") InstallationType type, @SpringQueryMap Pageable pageable);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "metasync",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  void createMetasync(@RequestBody MetasyncHistory metasyncHistory);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "metasync",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<MetasyncHistory> listMetasync(@SpringQueryMap Pageable pageable);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{installationKey}/metasync",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<MetasyncHistory> listMetasync(
      @PathVariable("installationKey") UUID installationKey, @SpringQueryMap Pageable pageable);

  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Installation> list(@SpringQueryMap InstallationRequestSearchParams searchParams);
}
