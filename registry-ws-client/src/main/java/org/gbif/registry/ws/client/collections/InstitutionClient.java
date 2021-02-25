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
package org.gbif.registry.ws.client.collections;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.duplicates.DuplicatesRequest;
import org.gbif.api.model.collections.duplicates.DuplicatesResult;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.InstitutionService;

import java.util.List;

import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("grscicoll/institution")
public interface InstitutionClient
    extends ExtendedBaseCollectionEntityClient<Institution>, InstitutionService {

  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Institution> list(@SpringQueryMap InstitutionSearchRequest searchRequest);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "deleted",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Institution> listDeleted(@SpringQueryMap Pageable page);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "suggest",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  List<KeyCodeNameResult> suggest(@RequestParam(value = "q", required = false) String q);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "possibleDuplicates",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  DuplicatesResult findPossibleDuplicates(@SpringQueryMap DuplicatesRequest request);
}
