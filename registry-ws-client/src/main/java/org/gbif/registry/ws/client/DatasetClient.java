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

import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Network;
import org.gbif.api.service.registry.DatasetService;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient("DatasetClient")
public interface DatasetClient extends DatasetService {

  @RequestMapping(
      method = RequestMethod.POST,
      value = "dataset/{key}/comment",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  int addComment(@NotNull @PathVariable("key") UUID key, @RequestBody @NotNull Comment comment);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "dataset/{key}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  Dataset get(@PathVariable("key") UUID key);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "dataset/{key}/document",
      produces = MediaType.APPLICATION_XML_VALUE)
  @ResponseBody
  @Override
  InputStream getMetadataDocument(@PathVariable("key") UUID key);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "dataset/{key}/networks",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  List<Network> listNetworks(@PathVariable("key") UUID key);
}
