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
import org.gbif.api.model.directory.Person;
import org.gbif.api.service.directory.PersonService;
import org.gbif.registry.directory.client.config.FeignClientConfiguration;
import org.gbif.ws.WebApplicationException;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Qualifier("personClient")
@FeignClient(
    value = "PersonClient",
    url = "${directory.ws.url}",
    configuration = FeignClientConfiguration.class)
public interface PersonClient extends PersonService {

  @RequestMapping(
      method = RequestMethod.GET,
      value = "person/{id}",
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseBody
  @Override
  Person get(@NotNull @PathVariable("id") Integer id);

  @Override
  default PagingResponse<Person> list(String query, Pageable pageable) {
    throw new WebApplicationException(
        "Person list by client is not supported", HttpStatus.BAD_REQUEST);
  }

  @Override
  default void delete(Integer id) {
    throw new WebApplicationException(
        "Person delete by client is not supported", HttpStatus.BAD_REQUEST);
  }

  @Override
  default Person create(Person entity) {
    throw new WebApplicationException(
        "Person create by client is not supported", HttpStatus.BAD_REQUEST);
  }

  @Override
  default void update(Person entity) {
    throw new WebApplicationException(
        "Person update by client is not supported", HttpStatus.BAD_REQUEST);
  }
}
