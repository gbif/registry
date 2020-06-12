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

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.registry.CommentService;
import org.gbif.api.service.registry.IdentifierService;
import org.gbif.api.service.registry.MachineTagService;
import org.gbif.api.service.registry.TagService;
import org.gbif.api.vocabulary.TagName;
import org.gbif.api.vocabulary.TagNamespace;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

public interface BaseCollectionEntityClient<
        T extends CollectionEntity & Taggable & Identifiable & MachineTaggable>
    extends CrudClient<T>, TagService, IdentifierService, MachineTagService, CommentService {

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

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/machineTag",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  int addMachineTag(@PathVariable("key") UUID key, @RequestBody MachineTag machineTag);

  @Override
  default int addMachineTag(UUID key, TagName tagName, String value) {
    MachineTag machineTag = MachineTag.newInstance(tagName, value);
    return addMachineTag(key, machineTag);
  }

  @Override
  default int addMachineTag(UUID key, String namespace, String name, String value) {
    MachineTag machineTag = new MachineTag();
    machineTag.setNamespace(namespace);
    machineTag.setName(name);
    machineTag.setValue(value);
    return addMachineTag(key, machineTag);
  }

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}/machineTag/{machineTagKey}")
  @Override
  void deleteMachineTag(
      @PathVariable("key") UUID key, @PathVariable("machineTagKey") int machineTagKey);

  @Override
  default void deleteMachineTags(UUID key, TagNamespace tagNamespace) {
    deleteMachineTags(key, tagNamespace.getNamespace());
  }

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}/machineTag/{namespace}")
  @Override
  void deleteMachineTags(
      @PathVariable("key") UUID key, @PathVariable("namespace") String namespace);

  @Override
  default void deleteMachineTags(UUID key, TagName tagName) {
    deleteMachineTags(key, tagName.getNamespace().getNamespace(), tagName.getName());
  }

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

  @Override
  default int addTag(UUID key, String value) {
    Tag tag = new Tag();
    tag.setValue(value);
    return addTag(key, tag);
  }

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
}
