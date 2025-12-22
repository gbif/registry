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
import feign.Body;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.*;
import org.gbif.api.model.registry.search.RequestSearchParams;
import org.gbif.api.service.registry.NetworkEntityService;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.TagName;
import org.gbif.api.vocabulary.TagNamespace;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Headers("Accept: application/json")
public interface NetworkEntityClient<T extends NetworkEntity> extends NetworkEntityService<T> {

  @RequestLine("POST")
  @Headers("Content-Type: application/json")
  @Body("%7Bentity%7D") // Feign body placeholder
  UUID create(T entity);

  @RequestLine("DELETE {key}")
  void delete(@Param("key") UUID key);

  default void update(T entity) {
    updateResource(entity.getKey(), entity);
  }

  @RequestLine("PUT {key}")
  @Headers("Content-Type: application/json")
  @Body("%7Bentity%7D")
  void updateResource(@Param("key") UUID key, T entity);

  @RequestLine("GET {key}")
  T get(@Param("key") UUID key);

  @RequestLine("POST titles")
  @Headers("Content-Type: application/json")
  Map<UUID, String> getTitles(Collection<UUID> collection);

  default PagingResponse<T> search(String query, Pageable page) {
    RequestSearchParams params = new RequestSearchParams();
    params.setQ(query);
    params.setPage(page);
    return list(params);
  }

  @RequestLine("POST {key}/tag")
  @Headers("Content-Type: application/json")
  int addTag(@Param("key") UUID key, Tag tag);

  default int addTag(UUID key, String value) {
    Tag tag = new Tag();
    tag.setValue(value);
    return addTag(key, tag);
  }

  @RequestLine("DELETE {key}/tag/{tagKey}")
  void deleteTag(@Param("key") UUID key, @Param("tagKey") int tagKey);

  @RequestLine("GET {key}/tag?owner={owner}")
  List<Tag> listTags(@Param("key") UUID key, @Param("owner") String owner);

  @RequestLine("POST {key}/contact")
  @Headers("Content-Type: application/json")
  int addContact(@Param("key") UUID key, Contact contact);

  @RequestLine("PUT {key}/contact")
  @Headers("Content-Type: application/json")
  void updateContact(@Param("key") UUID key, Contact contact);

  @RequestLine("DELETE {key}/contact/{contactKey}")
  void deleteContact(@Param("key") UUID key, @Param("contactKey") int contactKey);

  @RequestLine("GET {key}/contact")
  List<Contact> listContacts(@Param("key") UUID key);

  @RequestLine("POST {key}/endpoint")
  @Headers("Content-Type: application/json")
  int addEndpoint(@Param("key") UUID key, Endpoint endpoint);

  @RequestLine("DELETE {key}/endpoint/{endpointKey}")
  void deleteEndpoint(@Param("key") UUID key, @Param("endpointKey") int endpointKey);

  @RequestLine("GET {key}/endpoint")
  List<Endpoint> listEndpoints(@Param("key") UUID key);

  @RequestLine("POST {key}/machineTag")
  @Headers("Content-Type: application/json")
  int addMachineTag(@Param("key") UUID key, MachineTag machineTag);

  default int addMachineTag(UUID key, TagName tagName, String value) {
    MachineTag mt = MachineTag.newInstance(tagName, value);
    return addMachineTag(key, mt);
  }

  default int addMachineTag(UUID key, String namespace, String name, String value) {
    MachineTag mt = new MachineTag();
    mt.setNamespace(namespace);
    mt.setName(name);
    mt.setValue(value);
    return addMachineTag(key, mt);
  }

  @RequestLine("DELETE {key}/machineTag/{machineTagKey}")
  void deleteMachineTag(@Param("key") UUID key, @Param("machineTagKey") int machineTagKey);

  @RequestLine("DELETE {key}/machineTag/{namespace}")
  void deleteMachineTags(@Param("key") UUID key, @Param("namespace") String namespace);

  default void deleteMachineTags(UUID key, TagNamespace tagNamespace) {
    deleteMachineTags(key, tagNamespace.getNamespace());
  }

  @RequestLine("DELETE {key}/machineTag/{namespace}/{name}")
  void deleteMachineTags(@Param("key") UUID key, @Param("namespace") String namespace, @Param("name") String name);

  default void deleteMachineTags(UUID key, TagName tagName) {
    deleteMachineTags(key, tagName.getNamespace().getNamespace(), tagName.getName());
  }

  @RequestLine("GET {key}/machineTag")
  List<MachineTag> listMachineTags(@Param("key") UUID key);

  default PagingResponse<T> listByMachineTag(String namespace, String name, String value, Pageable page) {
    RequestSearchParams params = new RequestSearchParams();
    params.setMachineTagNamespace(namespace);
    params.setMachineTagName(name);
    params.setMachineTagValue(value);
    params.setPage(page);
    return list(params);
  }

  @RequestLine("POST {key}/comment")
  @Headers("Content-Type: application/json")
  int addComment(@Param("key") UUID key, Comment comment);

  @RequestLine("DELETE {key}/comment/{commentKey}")
  void deleteComment(@Param("key") UUID key, @Param("commentKey") int commentKey);

  @RequestLine("GET {key}/comment")
  List<Comment> listComments(@Param("key") UUID key);

  @RequestLine("POST {key}/identifier")
  @Headers("Content-Type: application/json")
  int addIdentifier(@Param("key") UUID key, Identifier identifier);

  @RequestLine("DELETE {key}/identifier/{identifierKey}")
  void deleteIdentifier(@Param("key") UUID key, @Param("identifierKey") int identifierKey);

  @RequestLine("GET {key}/identifier")
  List<Identifier> listIdentifiers(@Param("key") UUID key);

  default PagingResponse<T> listByIdentifier(IdentifierType type, String identifier, Pageable page) {
    RequestSearchParams params = new RequestSearchParams();
    params.setIdentifierType(type);
    params.setIdentifier(identifier);
    params.setPage(page);
    return list(params);
  }

  default PagingResponse<T> listByIdentifier(String identifier, Pageable page) {
    RequestSearchParams params = new RequestSearchParams();
    params.setIdentifier(identifier);
    params.setPage(page);
    return list(params);
  }

  default PagingResponse<T> list(Pageable page) {
    RequestSearchParams params = new RequestSearchParams();
    params.setPage(page);
    return list(params);
  }

  @RequestLine("GET")
  PagingResponse<T> list(RequestSearchParams searchParams);
}
