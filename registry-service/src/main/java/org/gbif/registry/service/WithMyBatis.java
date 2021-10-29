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
package org.gbif.registry.service;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.CommentableMapper;
import org.gbif.registry.persistence.mapper.ContactMapper;
import org.gbif.registry.persistence.mapper.ContactableMapper;
import org.gbif.registry.persistence.mapper.EndpointMapper;
import org.gbif.registry.persistence.mapper.EndpointableMapper;
import org.gbif.registry.persistence.mapper.IdentifiableMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.MachineTaggableMapper;
import org.gbif.registry.persistence.mapper.NetworkEntityMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.TaggableMapper;

import java.util.UUID;

import javax.annotation.Nullable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Service
public class WithMyBatis {

  private static final String CREATE_ERROR_MESSAGE =
      "Unable to create an entity which already has a key";

  @Transactional
  public <T extends NetworkEntity> UUID create(NetworkEntityMapper<T> mapper, T entity) {
    checkArgument(entity.getKey() == null, CREATE_ERROR_MESSAGE);
    // REVIEW: If this call fails the entity will have been modified anyway! We could make a copy
    // and return that instead
    entity.setKey(UUID.randomUUID());
    mapper.create(entity);
    return entity.getKey();
  }

  @Transactional
  public <T extends NetworkEntity> void update(NetworkEntityMapper<T> mapper, T entity) {
    checkNotNull(entity, "Unable to update an entity when it is not provided");
    T existing = mapper.get(entity.getKey());
    checkNotNull(existing, "Unable to update a non existing entity");

    if (existing.getDeleted() != null) {
      // allow updates ONLY if they are undeleting too
      checkArgument(
          entity.getDeleted() == null,
          "Unable to update a previously deleted entity unless you clear the deletion timestamp");
    } else {
      // do not allow deletion here (for safety) - we have an explicity deletion service
      checkArgument(
          entity.getDeleted() == null,
          "Cannot delete using the update service.  Use the deletion service");
    }

    mapper.update(entity);
  }

  /**
   * The simple search option of the list.
   *
   * @param mapper To use for the search
   * @param query A simple query string such as "Pontaurus"
   * @param page To support paging
   * @return A paging response
   */
  public <T extends NetworkEntity> PagingResponse<T> search(
      NetworkEntityMapper<T> mapper, String query, Pageable page) {
    checkNotNull(page, "To search you must supply a page");
    long total = mapper.count(query);
    return new PagingResponse<>(
        page.getOffset(), page.getLimit(), total, mapper.search(query, page));
  }

  public <T extends NetworkEntity> PagingResponse<T> list(
      NetworkEntityMapper<T> mapper, Pageable page) {
    long total = mapper.count();
    return new PagingResponse<>(page.getOffset(), page.getLimit(), total, mapper.list(page));
  }

  public <T extends NetworkEntity> PagingResponse<T> listByIdentifier(
      NetworkEntityMapper<T> mapper,
      @Nullable IdentifierType type,
      String identifier,
      Pageable page) {
    checkNotNull(page, "To list by identifier you must supply a page");
    checkNotNull(identifier, "To list by identifier you must supply an identifier");
    long total = mapper.countByIdentifier(type, identifier);
    return new PagingResponse<>(
        page.getOffset(), page.getLimit(), total, mapper.listByIdentifier(type, identifier, page));
  }

  @Transactional
  public int addComment(
      CommentMapper commentMapper,
      CommentableMapper commentableMapper,
      UUID targetEntityKey,
      Comment comment) {
    checkArgument(comment.getKey() == null, CREATE_ERROR_MESSAGE);
    commentMapper.createComment(comment);
    commentableMapper.addComment(targetEntityKey, comment.getKey());
    return comment.getKey();
  }

  public int addMachineTag(
      MachineTagMapper machineTagMapper,
      MachineTaggableMapper machineTaggableMapper,
      UUID targetEntityKey,
      MachineTag machineTag) {
    checkArgument(machineTag.getKey() == null, CREATE_ERROR_MESSAGE);
    machineTagMapper.createMachineTag(machineTag);
    machineTaggableMapper.addMachineTag(targetEntityKey, machineTag.getKey());
    return machineTag.getKey();
  }

  @SuppressWarnings("unchecked")
  public <T> PagingResponse<T> listByMachineTag(
      MachineTaggableMapper mapper,
      String namespace,
      @Nullable String name,
      @Nullable String value,
      Pageable page) {
    checkNotNull(page, "To list by machine tag you must supply a page");
    checkNotNull(namespace, "To list by machine tag you must supply a namespace");
    long total = mapper.countByMachineTag(namespace, name, value);
    return new PagingResponse<>(
        page.getOffset(),
        page.getLimit(),
        total,
        mapper.listByMachineTag(namespace, name, value, page));
  }

  @Transactional
  public int addTag(
      TagMapper tagMapper, TaggableMapper taggableMapper, UUID targetEntityKey, Tag tag) {
    // Mybatis needs an object to set the key on
    tagMapper.createTag(tag);
    taggableMapper.addTag(targetEntityKey, tag.getKey());
    return tag.getKey();
  }

  @Transactional
  public int addContact(
      ContactMapper contactMapper,
      ContactableMapper contactableMapper,
      UUID targetEntityKey,
      Contact contact) {
    checkArgument(contact.getKey() == null, CREATE_ERROR_MESSAGE);
    contactMapper.createContact(contact);
    // is this a primary contact? We need to make sure it only exists once per type
    if (contact.isPrimary()) {
      contactableMapper.updatePrimaryContacts(targetEntityKey, contact.getType());
    }
    contactableMapper.addContact(
        targetEntityKey, contact.getKey(), contact.getType(), contact.isPrimary());
    return contact.getKey();
  }

  @Transactional
  public int updateContact(
      ContactMapper contactMapper,
      ContactableMapper contactableMapper,
      UUID targetEntityKey,
      Contact contact) {
    checkArgument(contact.getKey() != null, "Unable to update an entity with no key");
    // null safe checking follows
    checkArgument(
        Boolean.TRUE.equals(contactableMapper.areRelated(targetEntityKey, contact.getKey())),
        "The provided contact is not connected to the given entity");
    // is this a primary contact? We need to make sure it only exists once per type
    if (contact.isPrimary()) {
      contactableMapper.updatePrimaryContacts(targetEntityKey, contact.getType());
    }
    contactMapper.updateContact(contact);
    // update the type and is_primary (is_primary will have been set to false by
    // updatePrimaryContacts above)
    contactableMapper.updateContact(
        targetEntityKey, contact.getKey(), contact.getType(), contact.isPrimary());
    return contact.getKey();
  }

  @Transactional
  public int addEndpoint(
      EndpointMapper endpointMapper,
      EndpointableMapper endpointableMapper,
      UUID targetEntityKey,
      Endpoint endpoint,
      MachineTagMapper machineTagMapper) {
    checkArgument(endpoint.getKey() == null, CREATE_ERROR_MESSAGE);
    endpointMapper.createEndpoint(endpoint);
    endpointableMapper.addEndpoint(targetEntityKey, endpoint.getKey());

    for (MachineTag machineTag : endpoint.getMachineTags()) {
      machineTag.setCreatedBy(endpoint.getCreatedBy());
      addMachineTag(machineTagMapper, endpointMapper, endpoint.getKey(), machineTag);
    }
    return endpoint.getKey();
  }

  /**
   * Private scoped to avoid misuse. Endpoints break the general principle and DO persist nested
   * entities, and can safely do so because they are immutable.
   */
  private static void addMachineTag(
      MachineTagMapper machineTagMapper,
      EndpointMapper endpointMapper,
      Integer endpointKey,
      MachineTag machineTag) {
    machineTagMapper.createMachineTag(machineTag);
    endpointMapper.addMachineTag(endpointKey, machineTag.getKey());
  }

  public void deleteEndpoint(
      EndpointableMapper endpointableMapper, UUID targetEntityKey, int endpointKey) {
    endpointableMapper.deleteEndpoint(targetEntityKey, endpointKey);
  }

  @Transactional
  public int addIdentifier(
      IdentifierMapper identifierMapper,
      IdentifiableMapper identifiableMapper,
      UUID targetEntityKey,
      Identifier identifier) {
    checkArgument(identifier.getKey() == null, CREATE_ERROR_MESSAGE);
    identifierMapper.createIdentifier(identifier);
    identifiableMapper.addIdentifier(targetEntityKey, identifier.getKey());
    return identifier.getKey();
  }
}
