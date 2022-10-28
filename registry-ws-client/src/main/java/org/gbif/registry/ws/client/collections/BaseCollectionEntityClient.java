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
package org.gbif.registry.ws.client.collections;

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.MasterSourceMetadata;
import org.gbif.api.model.collections.OccurrenceMapping;
import org.gbif.api.model.collections.SourceableField;
import org.gbif.api.model.collections.duplicates.DuplicatesRequest;
import org.gbif.api.model.collections.duplicates.DuplicatesResult;
import org.gbif.api.model.collections.merge.MergeParams;
import org.gbif.api.model.collections.suggestions.ApplySuggestionResult;
import org.gbif.api.model.collections.suggestions.ChangeSuggestion;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.vocabulary.TagName;
import org.gbif.api.vocabulary.TagNamespace;

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

public interface BaseCollectionEntityClient<
        T extends CollectionEntity & Taggable & Identifiable & MachineTaggable,
        R extends ChangeSuggestion<T>>
    extends CrudClient<T> {

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/identifier",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  int addIdentifier(@PathVariable("key") UUID key, @RequestBody Identifier identifier);

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}/identifier/{identifierKey}")
  void deleteIdentifier(
      @PathVariable("key") UUID key, @PathVariable("identifierKey") int identifierKey);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/identifier",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  List<Identifier> listIdentifiers(@PathVariable("key") UUID key);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/machineTag",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  int addMachineTag(@PathVariable("key") UUID key, @RequestBody MachineTag machineTag);

  default int addMachineTag(UUID key, TagName tagName, String value) {
    MachineTag machineTag = MachineTag.newInstance(tagName, value);
    return addMachineTag(key, machineTag);
  }

  default int addMachineTag(UUID key, String namespace, String name, String value) {
    MachineTag machineTag = new MachineTag();
    machineTag.setNamespace(namespace);
    machineTag.setName(name);
    machineTag.setValue(value);
    return addMachineTag(key, machineTag);
  }

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}/machineTag/{machineTagKey}")
  void deleteMachineTag(
      @PathVariable("key") UUID key, @PathVariable("machineTagKey") int machineTagKey);

  default void deleteMachineTags(UUID key, TagNamespace tagNamespace) {
    deleteMachineTags(key, tagNamespace.getNamespace());
  }

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}/machineTag/{namespace}")
  void deleteMachineTags(
      @PathVariable("key") UUID key, @PathVariable("namespace") String namespace);

  default void deleteMachineTags(UUID key, TagName tagName) {
    deleteMachineTags(key, tagName.getNamespace().getNamespace(), tagName.getName());
  }

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}/machineTag/{namespace}/{name}")
  void deleteMachineTags(
      @PathVariable("key") UUID key,
      @PathVariable("namespace") String namespace,
      @PathVariable("name") String name);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/machineTag",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  List<MachineTag> listMachineTags(@PathVariable("key") UUID key);

  default int addTag(UUID key, String value) {
    Tag tag = new Tag();
    tag.setValue(value);
    return addTag(key, tag);
  }

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/tag",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  int addTag(@PathVariable("key") UUID key, @RequestBody Tag tag);

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}/tag/{tagKey}")
  void deleteTag(@PathVariable("key") UUID key, @PathVariable("tagKey") int tagKey);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/tag",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  List<Tag> listTags(
      @PathVariable("key") UUID key, @RequestParam(value = "owner", required = false) String owner);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/comment",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  int addComment(@PathVariable("key") UUID key, @RequestBody Comment comment);

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}/comment/{commentKey}")
  void deleteComment(@PathVariable("key") UUID key, @PathVariable("commentKey") int commentKey);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/comment",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  List<Comment> listComments(@PathVariable("key") UUID key);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/contact",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  void addContact(@PathVariable("key") UUID key, @RequestBody UUID personKey);

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}/contact/{personKey}")
  void removeContact(@PathVariable("key") UUID key, @PathVariable("personKey") UUID personKey);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/contactPerson",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  int addContactPerson(@PathVariable("key") UUID entityKey, @RequestBody Contact contact);

  @RequestMapping(
      method = RequestMethod.PUT,
      value = "{key}/contactPerson/{contactKey}",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  void updateContactPersonResource(
      @PathVariable("key") UUID entityKey,
      @PathVariable("contactKey") int contactKey,
      @RequestBody Contact contact);

  default void updateContactPerson(UUID entityKey, Contact contact) {
    updateContactPersonResource(entityKey, contact.getKey(), contact);
  }

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}/contactPerson/{contactKey}")
  void removeContactPerson(
      @PathVariable("key") UUID entityKey, @PathVariable("contactKey") int contactKey);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/contactPerson",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  List<Contact> listContactPersons(@PathVariable("key") UUID key);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/occurrenceMapping",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  int addOccurrenceMapping(
      @PathVariable("key") UUID key, @RequestBody OccurrenceMapping occurrenceMapping);

  @RequestMapping(
      method = RequestMethod.DELETE,
      value = "{key}/occurrenceMapping/{occurrenceMappingKey}")
  void deleteOccurrenceMapping(
      @PathVariable("key") UUID key,
      @PathVariable("occurrenceMappingKey") int occurrenceMappingKey);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/occurrenceMapping",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  List<OccurrenceMapping> listOccurrenceMappings(@PathVariable("key") UUID key);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "possibleDuplicates",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  DuplicatesResult findPossibleDuplicates(@SpringQueryMap DuplicatesRequest request);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/merge",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  void merge(@PathVariable("key") UUID entityKey, @RequestBody MergeParams params);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "changeSuggestion",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  int createChangeSuggestion(@RequestBody R createSuggestion);

  @RequestMapping(
      method = RequestMethod.PUT,
      value = "changeSuggestion/{key}",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  void updateChangeSuggestion(@PathVariable("key") int key, @RequestBody R suggestion);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "changeSuggestion/{key}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  R getChangeSuggestion(@PathVariable("key") int key);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "changeSuggestion",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<R> listChangeSuggestion(
      @RequestParam(value = "status", required = false) Status status,
      @RequestParam(value = "type", required = false) Type type,
      @RequestParam(value = "proposerEmail", required = false) String proposerEmail,
      @RequestParam(value = "entityKey", required = false) UUID entityKey,
      @SpringQueryMap Pageable page);

  @RequestMapping(method = RequestMethod.PUT, value = "changeSuggestion/{key}/discard")
  void discardChangeSuggestion(@PathVariable("key") int key);

  @RequestMapping(
      method = RequestMethod.PUT,
      value = "changeSuggestion/{key}/apply",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  ApplySuggestionResult applyChangeSuggestion(@PathVariable("key") int key);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "sourceableFields",
      produces = MediaType.APPLICATION_JSON_VALUE)
  List<SourceableField> getSourceableFields();

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/masterSourceMetadata",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  int addMasterSourceMetadata(
      @PathVariable("key") UUID entityKey, @RequestBody MasterSourceMetadata masterSourceMetadata);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/masterSourceMetadata",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  MasterSourceMetadata getMasterSourceMetadata(@PathVariable("key") UUID entityKey);

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}/masterSourceMetadata")
  void deleteMasterSourceMetadata(@PathVariable("key") UUID entityKey);
}
