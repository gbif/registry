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

import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
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
import java.util.Map;
import java.util.UUID;

public interface BaseCollectionEntityClient<
  T extends CollectionEntity & Taggable & Identifiable & MachineTaggable,
  R extends ChangeSuggestion<T>>
  extends CrudClient<T> {

  /* ---------- Identifiers ---------- */

  @RequestLine("POST /{key}/identifier")
  @Headers("Content-Type: application/json")
  int addIdentifier(@Param("key") UUID key, Identifier identifier);

  @RequestLine("DELETE /{key}/identifier/{identifierKey}")
  void deleteIdentifier(@Param("key") UUID key, @Param("identifierKey") int identifierKey);

  @RequestLine("GET /{key}/identifier")
  @Headers("Accept: application/json")
  List<Identifier> listIdentifiers(@Param("key") UUID key);

  @RequestLine("PUT /{key}/identifier/{identifierKey}")
  @Headers("Content-Type: application/json")
  int updateIdentifier(
    @Param("key") UUID entityKey,
    @Param("identifierKey") Integer identifierKey,
    Map<String, Boolean> isPrimaryMap);

  /* ---------- Machine tags ---------- */

  @RequestLine("POST /{key}/machineTag")
  @Headers("Content-Type: application/json")
  int addMachineTag(@Param("key") UUID key, MachineTag machineTag);

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

  @RequestLine("DELETE /{key}/machineTag/{machineTagKey}")
  void deleteMachineTag(@Param("key") UUID key, @Param("machineTagKey") int machineTagKey);

  @RequestLine("DELETE /{key}/machineTag/{namespace}")
  void deleteMachineTags(@Param("key") UUID key, @Param("namespace") String namespace);

  @RequestLine("DELETE /{key}/machineTag/{namespace}/{name}")
  void deleteMachineTags(
    @Param("key") UUID key,
    @Param("namespace") String namespace,
    @Param("name") String name);

  default void deleteMachineTags(UUID key, TagNamespace namespace) {
    deleteMachineTags(key, namespace.getNamespace());
  }

  default void deleteMachineTags(UUID key, TagName tagName) {
    deleteMachineTags(
      key,
      tagName.getNamespace().getNamespace(),
      tagName.getName());
  }

  @RequestLine("GET /{key}/machineTag")
  @Headers("Accept: application/json")
  List<MachineTag> listMachineTags(@Param("key") UUID key);

  /* ---------- Tags ---------- */

  @RequestLine("POST /{key}/tag")
  @Headers("Content-Type: application/json")
  int addTag(@Param("key") UUID key, Tag tag);

  default int addTag(UUID key, String value) {
    Tag tag = new Tag();
    tag.setValue(value);
    return addTag(key, tag);
  }

  @RequestLine("DELETE /{key}/tag/{tagKey}")
  void deleteTag(@Param("key") UUID key, @Param("tagKey") int tagKey);

  @RequestLine("GET /{key}/tag?owner={owner}")
  @Headers("Accept: application/json")
  List<Tag> listTags(@Param("key") UUID key, @Param("owner") String owner);

  /* ---------- Comments ---------- */

  @RequestLine("POST /{key}/comment")
  @Headers("Content-Type: application/json")
  int addComment(@Param("key") UUID key, Comment comment);

  @RequestLine("DELETE /{key}/comment/{commentKey}")
  void deleteComment(@Param("key") UUID key, @Param("commentKey") int commentKey);

  @RequestLine("GET /{key}/comment")
  @Headers("Accept: application/json")
  List<Comment> listComments(@Param("key") UUID key);

  /* ---------- Contacts ---------- */

  @RequestLine("POST /{key}/contact")
  @Headers("Content-Type: application/json")
  void addContact(@Param("key") UUID key, UUID personKey);

  @RequestLine("DELETE /{key}/contact/{personKey}")
  void removeContact(@Param("key") UUID key, @Param("personKey") UUID personKey);

  @RequestLine("POST /{key}/contactPerson")
  @Headers("Content-Type: application/json")
  int addContactPerson(@Param("key") UUID entityKey, Contact contact);

  @RequestLine("PUT /{key}/contactPerson/{contactKey}")
  @Headers("Content-Type: application/json")
  void updateContactPersonResource(
    @Param("key") UUID entityKey,
    @Param("contactKey") int contactKey,
    Contact contact);

  default void updateContactPerson(UUID entityKey, Contact contact) {
    updateContactPersonResource(entityKey, contact.getKey(), contact);
  }

  @RequestLine("DELETE /{key}/contactPerson/{contactKey}")
  void removeContactPerson(@Param("key") UUID entityKey, @Param("contactKey") int contactKey);

  @RequestLine("GET /{key}/contactPerson")
  @Headers("Accept: application/json")
  List<Contact> listContactPersons(@Param("key") UUID key);

  /* ---------- Occurrence mappings ---------- */

  @RequestLine("POST /{key}/occurrenceMapping")
  @Headers("Content-Type: application/json")
  int addOccurrenceMapping(@Param("key") UUID key, OccurrenceMapping occurrenceMapping);

  @RequestLine("DELETE /{key}/occurrenceMapping/{occurrenceMappingKey}")
  void deleteOccurrenceMapping(
    @Param("key") UUID key,
    @Param("occurrenceMappingKey") int occurrenceMappingKey);

  @RequestLine("GET /{key}/occurrenceMapping")
  @Headers("Accept: application/json")
  List<OccurrenceMapping> listOccurrenceMappings(@Param("key") UUID key);

  /* ---------- Duplicates ---------- */

  @RequestLine("GET /possibleDuplicates")
  @Headers("Accept: application/json")
  DuplicatesResult findPossibleDuplicates(@QueryMap DuplicatesRequest request);

  /* ---------- Merge ---------- */

  @RequestLine("POST /{key}/merge")
  @Headers("Content-Type: application/json")
  void merge(@Param("key") UUID entityKey, MergeParams params);

  /* ---------- Change suggestions ---------- */

  @RequestLine("POST /changeSuggestion")
  @Headers("Content-Type: application/json")
  int createChangeSuggestion(R createSuggestion);

  @RequestLine("PUT /changeSuggestion/{key}")
  @Headers("Content-Type: application/json")
  void updateChangeSuggestion(@Param("key") int key, R suggestion);

  @RequestLine("GET /changeSuggestion/{key}")
  @Headers("Accept: application/json")
  R getChangeSuggestion(@Param("key") int key);

  @RequestLine(
    "GET /changeSuggestion"
      + "?status={status}"
      + "&type={type}"
      + "&proposerEmail={proposerEmail}"
      + "&entityKey={entityKey}"
      + "&ihIdentifier={ihIdentifier}"
      + "&country={country}")
  @Headers("Accept: application/json")
  PagingResponse<R> listChangeSuggestion(
    @Param("status") Status status,
    @Param("type") Type type,
    @Param("proposerEmail") String proposerEmail,
    @Param("entityKey") UUID entityKey,
    @Param("ihIdentifier") String ihIdentifier,
    @Param("country") String country,
    @QueryMap Pageable page);

  @RequestLine("PUT /changeSuggestion/{key}/discard")
  void discardChangeSuggestion(@Param("key") int key);

  @RequestLine("PUT /changeSuggestion/{key}/apply")
  @Headers("Accept: application/json")
  ApplySuggestionResult applyChangeSuggestion(@Param("key") int key);

  /* ---------- Source metadata ---------- */

  @RequestLine("GET /sourceableFields")
  @Headers("Accept: application/json")
  List<SourceableField> getSourceableFields();

  @RequestLine("POST /{key}/masterSourceMetadata")
  @Headers("Content-Type: application/json")
  int addMasterSourceMetadata(
    @Param("key") UUID entityKey,
    MasterSourceMetadata masterSourceMetadata);

  @RequestLine("GET /{key}/masterSourceMetadata")
  @Headers("Accept: application/json")
  MasterSourceMetadata getMasterSourceMetadata(@Param("key") UUID entityKey);

  @RequestLine("DELETE /{key}/masterSourceMetadata")
  void deleteMasterSourceMetadata(@Param("key") UUID entityKey);
}
