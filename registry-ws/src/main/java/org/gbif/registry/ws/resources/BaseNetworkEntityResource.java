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
package org.gbif.registry.ws.resources;

import org.gbif.api.annotation.Trim;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.PostPersist;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.service.registry.NetworkEntityService;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.TagName;
import org.gbif.api.vocabulary.TagNamespace;
import org.gbif.registry.events.ChangedComponentEvent;
import org.gbif.registry.events.CreateEvent;
import org.gbif.registry.events.DeleteEvent;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.UpdateEvent;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.BaseNetworkEntityMapper;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.ContactMapper;
import org.gbif.registry.persistence.mapper.EndpointMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.service.MapperServiceLocator;
import org.gbif.registry.security.EditorAuthorizationService;
import org.gbif.registry.security.SecurityContextCheck;
import org.gbif.registry.security.UserRoles;
import org.gbif.ws.WebApplicationException;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.APP_ROLE;
import static org.gbif.registry.security.UserRoles.EDITOR_ROLE;

/**
 * Provides a skeleton implementation of the following.
 *
 * <ul>
 *   <li>Base CRUD operations for a network entity
 *   <li>Comment operations
 *   <li>Contact operations (in addition to BaseNetworkEntityResource)
 *   <li>Endpoint operations (in addition to BaseNetworkEntityResource)
 *   <li>Identifier operations (in addition to BaseNetworkEntityResource2)
 *   <li>MachineTag operations
 *   <li>Tag operations
 * </ul>
 *
 * @param <T> The type of resource that is under CRUD
 */
@Validated
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class BaseNetworkEntityResource<T extends NetworkEntity> implements NetworkEntityService<T> {

  private static final Logger LOG = LoggerFactory.getLogger(BaseNetworkEntityResource.class);

  private final BaseNetworkEntityMapper<T> mapper;
  private final CommentMapper commentMapper;
  private final MachineTagMapper machineTagMapper;
  private final TagMapper tagMapper;
  private final ContactMapper contactMapper;
  private final EndpointMapper endpointMapper;
  private final IdentifierMapper identifierMapper;
  private final EventManager eventManager;
  private final EditorAuthorizationService userAuthService;
  private final WithMyBatis withMyBatis;
  private final Class<T> objectClass;

  protected BaseNetworkEntityResource(
      BaseNetworkEntityMapper<T> mapper,
      MapperServiceLocator mapperServiceLocator,
      Class<T> objectClass,
      EventManager eventManager,
      EditorAuthorizationService userAuthService,
      WithMyBatis withMyBatis) {
    this.mapper = mapper;
    this.commentMapper = mapperServiceLocator.getCommentMapper();
    this.machineTagMapper = mapperServiceLocator.getMachineTagMapper();
    this.tagMapper = mapperServiceLocator.getTagMapper();
    this.contactMapper = mapperServiceLocator.getContactMapper();
    this.endpointMapper = mapperServiceLocator.getEndpointMapper();
    this.identifierMapper = mapperServiceLocator.getIdentifierMapper();
    this.objectClass = objectClass;
    this.eventManager = eventManager;
    this.userAuthService = userAuthService;
    this.withMyBatis = withMyBatis;
  }

  /**
   * This method ensures that the caller is authorized to perform the action and then adds the
   * server controlled fields for createdBy and modifiedBy. It then creates the entity.
   *
   * @param entity entity that extends NetworkEntity
   * @return key of entity created
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public UUID create(@RequestBody @Trim @Valid @NotNull T entity, Authentication authentication) {
    final String nameFromContext = authentication != null ? authentication.getName() : null;
    // if not admin or app, verify rights
    if (!SecurityContextCheck.checkUserInRole(authentication, ADMIN_ROLE, APP_ROLE)) {
      boolean allowed = false;
      List<UUID> entityKeys = owningEntityKeys(entity);
      for (UUID entityKeyToBeAssessed : entityKeys) {
        if (entityKeyToBeAssessed == null) {
          throw new WebApplicationException(
              MessageFormat.format("User {0} is not allowed to modify entity", nameFromContext),
              HttpStatus.FORBIDDEN);
        }
        if (userAuthService.allowedToModifyEntity(nameFromContext, entityKeyToBeAssessed)) {
          allowed = true;
          break;
        }
      }
      if (!allowed) {
        throw new WebApplicationException(
            MessageFormat.format("User {0} is not allowed to modify entity", nameFromContext),
            HttpStatus.FORBIDDEN);
      }
    }
    entity.setCreatedBy(nameFromContext);
    entity.setModifiedBy(nameFromContext);

    return create(entity);
  }

  @Validated({PrePersist.class, Default.class})
  @Override
  public UUID create(T entity) {
    withMyBatis.create(mapper, entity);
    eventManager.post(CreateEvent.newInstance(entity, objectClass));
    return entity.getKey();
  }

  /**
   * This method ensures that the caller is authorized to perform the action, and then deletes the
   * entity. </br> Relax content-type to wildcard to allow angularjs.
   *
   * @param key key of entity to delete
   */
  @DeleteMapping("{key}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Transactional
  public void delete(@NotNull @PathVariable UUID key, Authentication authentication) {
    // the following lines allow to set the "modifiedBy" to the user who actually deletes the
    // entity.
    // the api delete(UUID) should be changed eventually
    T objectToDelete = get(key);

    if (objectToDelete != null) {
      objectToDelete.setModifiedBy(authentication.getName());
      withMyBatis.update(mapper, objectToDelete);
      delete(key);
    }
  }

  @Transactional
  @Override
  public void delete(UUID key) {
    T objectToDelete = get(key);
    mapper.delete(key);
    eventManager.post(DeleteEvent.newInstance(objectToDelete, objectClass));
  }

  @Nullable
  @Override
  public T get(@PathVariable UUID key) {
    return mapper.get(key);
  }

  // we do a post not get cause we expect large numbers of keys to be sent
  @PostMapping(value = "titles", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public Map<UUID, String> getTitles(@RequestBody @NotNull Collection<UUID> keys) {
    Map<UUID, String> titles = Maps.newHashMap();
    for (UUID key : keys) {
      titles.put(key, mapper.title(key));
    }
    return titles;
  }

  @Override
  public PagingResponse<T> list(Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return withMyBatis.list(mapper, page);
  }

  @Override
  public PagingResponse<T> search(String query, Pageable page) {
    page = page == null ? new PagingRequest() : page;
    // trim and handle null from given input
    String q = query != null ? Strings.emptyToNull(CharMatcher.WHITESPACE.trimFrom(query)) : query;
    return withMyBatis.search(mapper, q, page);
  }

  @Override
  public PagingResponse<T> listByIdentifier(IdentifierType type, String identifier, Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return withMyBatis.listByIdentifier(mapper, type, identifier, page);
  }

  @Override
  public PagingResponse<T> listByIdentifier(String identifier, Pageable page) {
    return listByIdentifier(null, identifier, page);
  }

  /**
   * This method ensures that the path variable for the key matches the entity's key, ensures that
   * the caller is authorized to perform the action and then adds the server controlled field
   * modifiedBy.
   *
   * @param key key of entity to update
   * @param entity entity that extends NetworkEntity
   */
  @PutMapping(
      value = {"", "{key}"},
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PostPersist.class, Default.class})
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public void update(
      @PathVariable UUID key,
      @RequestBody @NotNull @Trim @Valid T entity,
      Authentication authentication) {
    checkArgument(
        key.equals(entity.getKey()), "Provided entity must have the same key as the resource URL");
    entity.setModifiedBy(authentication.getName());
    update(entity);
  }

  @Validated({PostPersist.class, Default.class})
  @Override
  public void update(T entity) {
    T oldEntity = get(entity.getKey());
    withMyBatis.update(mapper, entity);
    // get complete entity with components populated, so subscribers of UpdateEvent can compare new
    // and old entities
    T newEntity = get(entity.getKey());
    eventManager.post(UpdateEvent.newInstance(newEntity, oldEntity, objectClass));
  }

  /**
   * This method ensures that the caller is authorized to perform the action and then adds the
   * server controlled fields for createdBy and modifiedBy.
   *
   * @param targetEntityKey key of target entity to add comment to
   * @param comment Comment to add
   * @return key of Comment created
   */
  @PostMapping(value = "{key}/comment", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE, APP_ROLE})
  public int addComment(
      @NotNull @PathVariable("key") UUID targetEntityKey,
      @RequestBody @NotNull @Trim @Valid Comment comment,
      Authentication authentication) {
    comment.setCreatedBy(authentication.getName());
    comment.setModifiedBy(authentication.getName());
    return addComment(targetEntityKey, comment);
  }

  @Validated({PrePersist.class, Default.class})
  @Override
  public int addComment(UUID targetEntityKey, Comment comment) {
    int key = withMyBatis.addComment(commentMapper, mapper, targetEntityKey, comment);
    eventManager.post(
        ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Comment.class));
    return key;
  }

  /**
   * This method ensures that the caller is authorized to perform the action, and then deletes the
   * Comment.
   *
   * @param targetEntityKey key of target entity to delete comment from
   * @param commentKey key of Comment to delete
   */
  @DeleteMapping("{key}/comment/{commentKey}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public void deleteComment(
      @PathVariable("key") UUID targetEntityKey, @PathVariable int commentKey) {
    mapper.deleteComment(targetEntityKey, commentKey);
    eventManager.post(
        ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Comment.class));
  }

  @GetMapping(value = "{key}/comment")
  @Override
  public List<Comment> listComments(@PathVariable("key") UUID targetEntityKey) {
    return mapper.listComments(targetEntityKey);
  }

  /**
   * Adding most machineTags is restricted based on the namespace. For some tags, it is restricted
   * based on the editing role as usual.
   *
   * @param targetEntityKey key of target entity to add MachineTag to
   * @param machineTag MachineTag to add
   * @return key of MachineTag created
   */
  @PostMapping(value = "{key}/machineTag", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Transactional
  public int addMachineTag(
      @PathVariable("key") UUID targetEntityKey,
      @RequestBody @NotNull @Trim @Valid MachineTag machineTag,
      Authentication authentication) {
    final String nameFromContext = authentication != null ? authentication.getName() : null;

    if (SecurityContextCheck.checkUserInRole(authentication, ADMIN_ROLE)
        || userAuthService.allowedToModifyNamespace(nameFromContext, machineTag.getNamespace())
        || (SecurityContextCheck.checkUserInRole(authentication, EDITOR_ROLE)
            && TagNamespace.GBIF_DEFAULT_TERM.getNamespace().equals(machineTag.getNamespace())
            && userAuthService.allowedToModifyDataset(nameFromContext, targetEntityKey))) {
      machineTag.setCreatedBy(nameFromContext);
      return addMachineTag(targetEntityKey, machineTag);
    } else {
      throw new WebApplicationException(
          MessageFormat.format("User {0} is not allowed to modify entity", nameFromContext),
          HttpStatus.FORBIDDEN);
    }
  }

  @Validated({PrePersist.class, Default.class})
  @Override
  public int addMachineTag(UUID targetEntityKey, MachineTag machineTag) {
    return withMyBatis.addMachineTag(machineTagMapper, mapper, targetEntityKey, machineTag);
  }

  @Override
  public int addMachineTag(UUID targetEntityKey, String namespace, String name, String value) {
    MachineTag machineTag = new MachineTag();
    machineTag.setNamespace(namespace);
    machineTag.setName(name);
    machineTag.setValue(value);
    return addMachineTag(targetEntityKey, machineTag);
  }

  @Override
  public int addMachineTag(UUID targetEntityKey, TagName tagName, String value) {
    MachineTag machineTag = MachineTag.newInstance(tagName, value);
    return addMachineTag(targetEntityKey, machineTag);
  }

  /**
   * The webservice method to delete a machine tag. Ensures that the caller is authorized to perform
   * the action by looking at the namespace.
   */
  @SuppressWarnings("unchecked")
  @DeleteMapping("{key}/machineTag/{machineTagKey:[0-9]+}")
  public void deleteMachineTagByMachineTagKey(
      @PathVariable("key") UUID targetEntityKey,
      @PathVariable("machineTagKey") int machineTagKey,
      Authentication authentication) {
    final String nameFromContext = authentication != null ? authentication.getName() : null;

    List<MachineTag> machineTags = mapper.listMachineTags(targetEntityKey);
    Optional<MachineTag> optMachineTag =
        machineTags.stream().filter(m -> m.getKey() == machineTagKey).findFirst();

    if (optMachineTag.isPresent()) {
      MachineTag machineTag = optMachineTag.get();

      if (SecurityContextCheck.checkUserInRole(authentication, ADMIN_ROLE)
          || userAuthService.allowedToModifyNamespace(nameFromContext, machineTag.getNamespace())
          || (SecurityContextCheck.checkUserInRole(authentication, EDITOR_ROLE)
              && TagNamespace.GBIF_DEFAULT_TERM.getNamespace().equals(machineTag.getNamespace())
              && userAuthService.allowedToModifyDataset(nameFromContext, targetEntityKey))) {
        deleteMachineTag(targetEntityKey, machineTagKey);

      } else {
        throw new WebApplicationException(
            MessageFormat.format("User {0} is not allowed to modify entity", nameFromContext),
            HttpStatus.FORBIDDEN);
      }
    } else {
      throw new WebApplicationException(
          MessageFormat.format(
              "Machine tag {0} was not found for the entity with key {1}",
              machineTagKey, targetEntityKey),
          HttpStatus.NOT_FOUND);
    }
  }

  /**
   * Deletes the MachineTag according to interface without security restrictions.
   *
   * @param targetEntityKey key of target entity to delete MachineTag from
   * @param machineTagKey key of MachineTag to delete
   */
  @Override
  public void deleteMachineTag(UUID targetEntityKey, int machineTagKey) {
    mapper.deleteMachineTag(targetEntityKey, machineTagKey);
  }

  /**
   * The webservice method to delete all machine tag in a namespace. Ensures that the caller is
   * authorized to perform the action by looking at the namespace.
   */
  @DeleteMapping("{key}/machineTag/{namespace:.*[^0-9]+.*}")
  public void deleteMachineTagsByNamespace(
      @PathVariable("key") UUID targetEntityKey,
      @PathVariable("namespace") String namespace,
      Authentication authentication) {
    final String nameFromContext = authentication != null ? authentication.getName() : null;

    if (!SecurityContextCheck.checkUserInRole(authentication, UserRoles.ADMIN_ROLE)
        && !userAuthService.allowedToModifyNamespace(nameFromContext, namespace)) {
      throw new WebApplicationException(
          MessageFormat.format("User {0} is not allowed to modify entity", nameFromContext),
          HttpStatus.FORBIDDEN);
    }
    deleteMachineTags(targetEntityKey, namespace);
  }

  @Override
  public void deleteMachineTags(UUID targetEntityKey, TagNamespace tagNamespace) {
    deleteMachineTags(targetEntityKey, tagNamespace.getNamespace());
  }

  @Override
  public void deleteMachineTags(UUID targetEntityKey, String namespace) {
    mapper.deleteMachineTags(targetEntityKey, namespace, null);
  }

  /**
   * The webservice method to delete all machine tag of a particular name in a namespace. Ensures
   * that the caller is authorized to perform the action by looking at the namespace.
   */
  @DeleteMapping("{key}/machineTag/{namespace}/{name}")
  public void deleteMachineTags(
      @PathVariable("key") UUID targetEntityKey,
      @PathVariable String namespace,
      @PathVariable String name,
      Authentication authentication) {
    final String nameFromContext = authentication != null ? authentication.getName() : null;

    if (!SecurityContextCheck.checkUserInRole(authentication, UserRoles.ADMIN_ROLE)
        && !userAuthService.allowedToModifyNamespace(nameFromContext, namespace)) {
      throw new WebApplicationException(
          MessageFormat.format("User {0} is not allowed to modify entity", nameFromContext),
          HttpStatus.FORBIDDEN);
    }
    deleteMachineTags(targetEntityKey, namespace, name);
  }

  @Override
  public void deleteMachineTags(UUID targetEntityKey, TagName tagName) {
    deleteMachineTags(targetEntityKey, tagName.getNamespace().getNamespace(), tagName.getName());
  }

  @Override
  public void deleteMachineTags(UUID targetEntityKey, String namespace, String name) {
    mapper.deleteMachineTags(targetEntityKey, namespace, name);
  }

  @SuppressWarnings("unchecked")
  @GetMapping("{key}/machineTag")
  @Override
  public List<MachineTag> listMachineTags(@PathVariable("key") UUID targetEntityKey) {
    return mapper.listMachineTags(targetEntityKey);
  }

  @Override
  public PagingResponse<T> listByMachineTag(
      String namespace, String name, String value, Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return withMyBatis.listByMachineTag(mapper, namespace, name, value, page);
  }

  /**
   * This method ensures that the caller is authorized to perform the action and then adds the
   * server controlled fields for createdBy.
   *
   * @param targetEntityKey key of target entity to add Tag to
   * @param tag Tag to add
   * @return key of Tag created
   */
  @PostMapping(value = "{key}/tag", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public int addTag(
      @PathVariable("key") UUID targetEntityKey,
      @RequestBody @NotNull @Valid Tag tag,
      Authentication authentication) {
    tag.setCreatedBy(authentication.getName());
    return addTag(targetEntityKey, tag);
  }

  @Override
  public int addTag(UUID targetEntityKey, String value) {
    Tag tag = new Tag();
    tag.setValue(value);
    return addTag(targetEntityKey, tag);
  }

  @Validated({PrePersist.class, Default.class})
  @Override
  public int addTag(UUID targetEntityKey, Tag tag) {
    int key = withMyBatis.addTag(tagMapper, mapper, targetEntityKey, tag);
    eventManager.post(ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Tag.class));
    return key;
  }

  /**
   * This method ensures that the caller is authorized to perform the action, and then deletes the
   * Tag.
   *
   * @param targetEntityKey key of target entity to delete Tag from
   * @param tagKey key of Tag to delete
   */
  @DeleteMapping("{key}/tag/{tagKey}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public void deleteTag(@PathVariable("key") UUID targetEntityKey, @PathVariable int tagKey) {
    mapper.deleteTag(targetEntityKey, tagKey);
    eventManager.post(ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Tag.class));
  }

  @GetMapping("{key}/tag")
  @Override
  public List<Tag> listTags(
      @PathVariable("key") UUID targetEntityKey,
      @RequestParam(value = "owner", required = false) String owner) {
    if (owner != null) {
      LOG.warn("Owner is not supported. Value: {}", owner);
    }
    return mapper.listTags(targetEntityKey);
  }

  /**
   * This method ensures that the caller is authorized to perform the action and then adds the
   * server controlled fields for createdBy and modifiedBy.
   *
   * @param targetEntityKey key of target entity to add Contact to
   * @param contact Contact to add
   * @param authentication SecurityContext (security related information)
   * @return key of Contact created
   */
  @PostMapping(value = "{key}/contact", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE, APP_ROLE})
  public int addContact(
      @PathVariable("key") UUID targetEntityKey,
      @RequestBody @NotNull @Trim @Valid Contact contact,
      Authentication authentication) {
    contact.setCreatedBy(authentication.getName());
    contact.setModifiedBy(authentication.getName());
    return addContact(targetEntityKey, contact);
  }

  @Validated({PrePersist.class, Default.class})
  @Override
  public int addContact(UUID targetEntityKey, Contact contact) {
    int key = withMyBatis.addContact(contactMapper, mapper, targetEntityKey, contact);
    eventManager.post(
        ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Contact.class));
    return key;
  }

  /**
   * This method ensures that the caller is authorized to perform the action and then adds the
   * server controlled field for modifiedBy.
   *
   * @param targetEntityKey key of target entity to update contact
   * @param contactKey key of Contact to update
   * @param contact updated Contact
   */
  @PutMapping(
      value = {"{key}/contact", "{key}/contact/{contactKey}"},
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PostPersist.class, Default.class})
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public void updateContact(
      @PathVariable("key") UUID targetEntityKey,
      @PathVariable(value = "contactKey", required = false) Integer contactKey,
      @RequestBody @NotNull @Trim @Valid Contact contact) {
    // for safety, and to match a nice RESTful URL structure
    if (contactKey != null) {
      Preconditions.checkArgument(
          contactKey.equals(contact.getKey()),
          "Provided contact (key) does not match the path provided");
    }
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    contact.setModifiedBy(authentication.getName());
    updateContact(targetEntityKey, contact);
  }

  @Validated({PostPersist.class, Default.class})
  @Override
  public void updateContact(UUID targetEntityKey, Contact contact) {
    withMyBatis.updateContact(contactMapper, mapper, targetEntityKey, contact);
    eventManager.post(
        ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Contact.class));
  }

  /**
   * This method ensures that the caller is authorized to perform the action.
   *
   * @param targetEntityKey key of target entity to delete Contact from
   * @param contactKey key of Contact to delete
   */
  @DeleteMapping("{key}/contact/{contactKey}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public void deleteContact(
      @PathVariable("key") UUID targetEntityKey, @PathVariable int contactKey) {
    mapper.deleteContact(targetEntityKey, contactKey);
    eventManager.post(
        ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Contact.class));
  }

  @GetMapping("{key}/contact")
  @Override
  public List<Contact> listContacts(@PathVariable("key") UUID targetEntityKey) {
    return mapper.listContacts(targetEntityKey);
  }

  /**
   * This method ensures that the caller is authorized to perform the action and then adds the
   * server controlled fields for createdBy and modifiedBy.
   *
   * @param targetEntityKey key of target entity to add Endpoint to
   * @param endpoint Endpoint to add
   * @return key of Endpoint created
   */
  @PostMapping(value = "{key}/endpoint", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public int addEndpoint(
      @PathVariable("key") UUID targetEntityKey,
      @RequestBody @NotNull @Trim @Valid Endpoint endpoint,
      Authentication authentication) {
    endpoint.setCreatedBy(authentication.getName());
    endpoint.setModifiedBy(authentication.getName());
    return addEndpoint(targetEntityKey, endpoint);
  }

  @Validated({PrePersist.class, Default.class})
  @Override
  public int addEndpoint(UUID targetEntityKey, Endpoint endpoint) {
    T oldEntity = get(targetEntityKey);
    int key =
        withMyBatis.addEndpoint(
            endpointMapper, mapper, targetEntityKey, endpoint, machineTagMapper);
    T newEntity = get(targetEntityKey);
    // posts an UpdateEvent instead of a ChangedComponentEvent, otherwise the crawler would have to
    // start subscribing
    // to ChangedComponentEvent instead just to detect when an endpoint has been added to a Dataset
    eventManager.post(UpdateEvent.newInstance(newEntity, oldEntity, objectClass));
    return key;
  }

  /**
   * This method ensures that the caller is authorized to perform the action, and then deletes the
   * Endpoint.
   *
   * @param targetEntityKey key of target entity to delete Endpoint from
   * @param endpointKey key of Endpoint to delete
   */
  @DeleteMapping("{key}/endpoint/{endpointKey}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public void deleteEndpoint(
      @PathVariable("key") UUID targetEntityKey,
      @PathVariable int endpointKey,
      Authentication authentication) {
    deleteEndpoint(targetEntityKey, endpointKey);
  }

  @Override
  public void deleteEndpoint(
      @PathVariable("key") UUID targetEntityKey, @PathVariable int endpointKey) {
    withMyBatis.deleteEndpoint(mapper, targetEntityKey, endpointKey);
    eventManager.post(
        ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Endpoint.class));
  }

  @GetMapping("{key}/endpoint")
  @Override
  public List<Endpoint> listEndpoints(@PathVariable("key") UUID targetEntityKey) {
    return mapper.listEndpoints(targetEntityKey);
  }

  /**
   * This method ensures that the caller is authorized to perform the action and then adds the
   * server controlled field for createdBy.
   *
   * @param targetEntityKey key of target entity to add Identifier to
   * @param identifier Identifier to add
   * @return key of Identifier created
   */
  @PostMapping(value = "{key}/identifier", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public int addIdentifier(
      @PathVariable("key") UUID targetEntityKey,
      @RequestBody @NotNull @Trim @Valid Identifier identifier,
      Authentication authentication) {
    identifier.setCreatedBy(authentication.getName());
    return addIdentifier(targetEntityKey, identifier);
  }

  @Validated({PrePersist.class, Default.class})
  @Override
  public int addIdentifier(UUID targetEntityKey, Identifier identifier) {
    int key = withMyBatis.addIdentifier(identifierMapper, mapper, targetEntityKey, identifier);
    eventManager.post(
        ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Identifier.class));
    return key;
  }

  /**
   * This method ensures that the caller is authorized to perform the action, and then deletes the
   * Identifier.
   *
   * @param targetEntityKey key of target entity to delete Identifier from
   * @param identifierKey key of Identifier to delete
   */
  @DeleteMapping("{key}/identifier/{identifierKey}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public void deleteIdentifier(
      @PathVariable("key") UUID targetEntityKey, @PathVariable("identifierKey") int identifierKey) {
    mapper.deleteIdentifier(targetEntityKey, identifierKey);
    eventManager.post(
        ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Identifier.class));
  }

  @GetMapping("{key}/identifier")
  @Override
  public List<Identifier> listIdentifiers(@PathVariable("key") UUID targetEntityKey) {
    return mapper.listIdentifiers(targetEntityKey);
  }

  /**
   * Override this method to extract the entity key that governs security rights for creating. If
   * null is returned only admins are allowed to create new entities which is the default.
   */
  protected List<UUID> owningEntityKeys(@NotNull T entity) {
    LOG.debug(
        "Entity {} with key {} has no owning entity keys", entity.getClass(), entity.getKey());
    return Collections.emptyList();
  }

  /**
   * Null safe builder to construct a paging response.
   *
   * @param page page to create response for, can be null
   */
  protected <D> PagingResponse<D> pagingResponse(Pageable page, Long count, List<D> result) {
    if (page == null) {
      // use default request
      page = new PagingRequest();
    }
    return new PagingResponse<>(page, count, result);
  }
}
