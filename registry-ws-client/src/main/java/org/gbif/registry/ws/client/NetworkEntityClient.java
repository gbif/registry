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
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.service.registry.NetworkEntityService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

public interface NetworkEntityClient<T extends NetworkEntity> extends NetworkEntityService<T> {

  @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  UUID create(@RequestBody T entity);

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}")
  @Override
  void delete(@PathVariable("key") UUID key);

  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<T> list(@SpringQueryMap Pageable page);

  @RequestMapping(method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  void update(@RequestBody T entity);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  T get(@PathVariable("key") UUID key);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "titles",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  Map<UUID, String> getTitles(@RequestBody Collection<UUID> collection);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/tag",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  int addTag(@PathVariable("key") UUID key, @RequestBody Tag tag);

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}/tag/{tagKey}")
  @Override
  void deleteTag(@PathVariable("key") UUID key, @PathVariable("tagKey") int tagKey);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/tag",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  List<Tag> listTags(
      @PathVariable("key") UUID key, @RequestParam(value = "owner", required = false) String owner);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/contact",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  int addContact(@PathVariable("key") UUID key, @RequestBody Contact contact);

  @RequestMapping(
      method = RequestMethod.PUT,
      value = "{key}/contact",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  void updateContact(@PathVariable("key") UUID key, @RequestBody Contact contact);

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}/contact/{contactKey}")
  @Override
  void deleteContact(@PathVariable("key") UUID key, @PathVariable("contactKey") int contactKey);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/contact",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  List<Contact> listContacts(@PathVariable("key") UUID key);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/endpoint",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  int addEndpoint(@PathVariable("key") UUID key, @RequestBody Endpoint endpoint);

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}/endpoint/{endpointKey}")
  @Override
  void deleteEndpoint(@PathVariable("key") UUID key, @PathVariable("endpointKey") int endpointKey);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/endpoint",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  List<Endpoint> listEndpoints(@PathVariable("key") UUID key);

  @RequestMapping(method = RequestMethod.POST, value = "{key}/machineTag", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  int addMachineTag(@PathVariable("key") UUID key, @RequestBody MachineTag machineTag);

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}/machineTag/{namespace}/{name}")
  @Override
  void deleteMachineTags(
    @PathVariable("key") UUID key,
    @PathVariable("namespace") String namespace,
    @PathVariable("name") String name);

  @RequestMapping(
    method = RequestMethod.GET,
    value = "{key}/machineTag",
    produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  List<MachineTag> listMachineTags(@PathVariable("key") UUID key);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/comment",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  int addComment(@PathVariable("key") UUID key, @RequestBody Comment comment);

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}/comment/{commentKey}")
  @Override
  void deleteComment(@PathVariable("key") UUID key, @PathVariable("commentKey") int commentKey);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/comment",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  List<Comment> listComments(@PathVariable("key") UUID key);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/identifier",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  int addIdentifier(@PathVariable("key") UUID key, @RequestBody Identifier identifier);

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}/identifier/{identifierKey}")
  @Override
  void deleteIdentifier(
      @PathVariable("key") UUID key, @PathVariable("identifierKey") int identifierKey);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/identifier",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  List<Identifier> listIdentifiers(@PathVariable("key") UUID key);
}
