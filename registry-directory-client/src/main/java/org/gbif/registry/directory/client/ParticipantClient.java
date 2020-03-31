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
package org.gbif.registry.directory.client;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.directory.Participant;
import org.gbif.api.service.directory.ParticipantService;
import org.gbif.ws.WebApplicationException;

import javax.validation.constraints.NotNull;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient("ParticipantClient")
public interface ParticipantClient extends ParticipantService {

  @RequestMapping(
      method = RequestMethod.GET,
      value = "participant/{id}",
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseBody
  @Override
  Participant get(@NotNull @PathVariable("id") Integer id);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "participant",
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseBody
  @Override
  PagingResponse<Participant> list(
      @RequestParam(value = "q", required = false) String query, @SpringQueryMap Pageable pageable);

  @Override
  default void delete(Integer id) {
    throw new WebApplicationException(
        "Participant delete by client is not supported", HttpStatus.BAD_REQUEST);
  }

  @Override
  default Participant create(Participant entity) {
    throw new WebApplicationException(
        "Participant create by client is not supported", HttpStatus.BAD_REQUEST);
  }

  @Override
  default void update(Participant entity) {
    throw new WebApplicationException(
        "Participant update by client is not supported", HttpStatus.BAD_REQUEST);
  }
}
