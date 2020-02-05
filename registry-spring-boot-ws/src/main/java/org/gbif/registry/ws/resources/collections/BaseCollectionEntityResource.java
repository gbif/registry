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
package org.gbif.registry.ws.resources.collections;

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.PrePersist;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.CrudService;
import org.gbif.api.service.registry.IdentifierService;
import org.gbif.api.service.registry.MachineTagService;
import org.gbif.api.service.registry.TagService;
import org.gbif.api.vocabulary.TagName;
import org.gbif.api.vocabulary.TagNamespace;
import org.gbif.registry.events.ChangedComponentEvent;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.collections.DeleteCollectionEntityEvent;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.BaseMapper;
import org.gbif.registry.ws.annotation.ValidateReturnedValue;
import org.gbif.registry.ws.security.EditorAuthorizationService;
import org.gbif.registry.ws.security.SecurityContextCheck;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.annotation.NullToNotFound;
import org.gbif.ws.annotation.Trim;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
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

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.GRSCICOLL_ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.GRSCICOLL_EDITOR_ROLE;

/** Base class to implement the CRUD methods of a {@link CollectionEntity}. */
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public abstract class BaseCollectionEntityResource<
        T extends CollectionEntity & Taggable & Identifiable & MachineTaggable>
    implements CrudService<T>, TagService, IdentifierService, MachineTagService {

  private static final Logger LOG = LoggerFactory.getLogger(BaseCollectionEntityResource.class);

  private final BaseMapper<T> baseMapper;
  private final Class<T> objectClass;
  private final TagMapper tagMapper;
  private final MachineTagMapper machineTagMapper;
  private final IdentifierMapper identifierMapper;
  private final EventManager eventManager;
  final EditorAuthorizationService userAuthService;
  private final WithMyBatis withMyBatis;

  protected BaseCollectionEntityResource(
      BaseMapper<T> baseMapper,
      TagMapper tagMapper,
      MachineTagMapper machineTagMapper,
      IdentifierMapper identifierMapper,
      EditorAuthorizationService userAuthService,
      EventManager eventManager,
      Class<T> objectClass,
      WithMyBatis withMyBatis) {
    this.baseMapper = baseMapper;
    this.tagMapper = tagMapper;
    this.machineTagMapper = machineTagMapper;
    this.identifierMapper = identifierMapper;
    this.eventManager = eventManager;
    this.objectClass = objectClass;
    this.userAuthService = userAuthService;
    this.withMyBatis = withMyBatis;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  public UUID create(@RequestBody @NotNull @Validated T entity, Authentication authentication) {
    final String username = authentication.getName();
    entity.setCreatedBy(username);
    entity.setModifiedBy(username);
    return create(entity);
  }

  @DeleteMapping("{key}")
  @Transactional
  @Secured({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  public void delete(@PathVariable @NotNull UUID key, Authentication authentication) {
    T entityToDelete = get(key);
    entityToDelete.setModifiedBy(authentication.getName());
    update(entityToDelete);

    delete(key);
  }

  @Transactional
  @Override
  public void delete(@NotNull UUID key) {
    T objectToDelete = get(key);
    baseMapper.delete(key);
    eventManager.post(DeleteCollectionEntityEvent.newInstance(objectToDelete, objectClass));
  }

  @GetMapping("{key}")
  @Nullable
  @NullToNotFound
  @ValidateReturnedValue
  @Override
  public T get(@PathVariable @NotNull UUID key) {
    return baseMapper.get(key);
  }

  @PutMapping(value = "{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Transactional
  @Secured({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  public void update(
      @PathVariable @NotNull UUID key, @RequestBody @NotNull @Trim @Validated T entity) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    checkArgument(
        key.equals(entity.getKey()), "Provided entity must have the same key as the resource URL");
    entity.setModifiedBy(authentication.getName());
    update(entity);
  }

  @PostMapping(value = "{key}/identifier", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Secured({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  public int addIdentifier(
      @PathVariable("key") @NotNull UUID entityKey,
      @RequestBody @NotNull Identifier identifier,
      Authentication authentication) {
    identifier.setCreatedBy(authentication.getName());
    return addIdentifier(entityKey, identifier);
  }

  @Override
  public int addIdentifier(
      @NotNull UUID entityKey,
      @Validated({PrePersist.class, Default.class}) @NotNull Identifier identifier) {
    int identifierKey =
        withMyBatis.addIdentifier(identifierMapper, baseMapper, entityKey, identifier);
    eventManager.post(ChangedComponentEvent.newInstance(entityKey, objectClass, Identifier.class));
    return identifierKey;
  }

  @DeleteMapping("{key}/identifier/{identifierKey}")
  @Secured({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  @Transactional
  @Override
  public void deleteIdentifier(
      @PathVariable("key") @NotNull UUID entityKey, @PathVariable int identifierKey) {
    baseMapper.deleteIdentifier(entityKey, identifierKey);
    eventManager.post(ChangedComponentEvent.newInstance(entityKey, objectClass, Identifier.class));
  }

  @GetMapping("{key}/identifier")
  @Nullable
  @ValidateReturnedValue
  @Override
  public List<Identifier> listIdentifiers(@PathVariable @NotNull UUID key) {
    return baseMapper.listIdentifiers(key);
  }

  @PostMapping(value = "{key}/tag", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Secured({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  public int addTag(
      @PathVariable("key") @NotNull UUID entityKey,
      @RequestBody @NotNull Tag tag,
      Authentication authentication) {
    tag.setCreatedBy(authentication.getName());
    return addTag(entityKey, tag);
  }

  @Override
  public int addTag(@NotNull UUID key, @NotNull String value) {
    Tag tag = new Tag();
    tag.setValue(value);
    return addTag(key, tag);
  }

  @Override
  public int addTag(
      @NotNull UUID entityKey, @NotNull @Validated({PrePersist.class, Default.class}) Tag tag) {
    int tagKey = withMyBatis.addTag(tagMapper, baseMapper, entityKey, tag);
    eventManager.post(ChangedComponentEvent.newInstance(entityKey, objectClass, Tag.class));
    return tagKey;
  }

  @DeleteMapping("{key}/tag/{tagKey}")
  @Secured({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  @Transactional
  @Override
  public void deleteTag(@PathVariable("key") @NotNull UUID entityKey, @PathVariable int tagKey) {
    baseMapper.deleteTag(entityKey, tagKey);
    eventManager.post(ChangedComponentEvent.newInstance(entityKey, objectClass, Tag.class));
  }

  @GetMapping("{key}/tag")
  @Nullable
  @ValidateReturnedValue
  @Override
  public List<Tag> listTags(
      @PathVariable("key") @NotNull UUID key,
      @RequestParam(value = "owner", required = false) @Nullable String owner) {
    if (owner != null) {
      LOG.warn("Owner is not supported. Passed value: {}", owner);
    }
    return baseMapper.listTags(key);
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
  @Trim
  @Transactional
  public int addMachineTag(
      @PathVariable("key") UUID targetEntityKey,
      @RequestBody @NotNull @Trim MachineTag machineTag,
      Authentication authentication) {
    final String nameFromContext = authentication != null ? authentication.getName() : null;

    if (SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_ADMIN_ROLE)
        || userAuthService.allowedToModifyNamespace(nameFromContext, machineTag.getNamespace())
        || (SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_EDITOR_ROLE)
            && TagNamespace.GBIF_DEFAULT_TERM.getNamespace().equals(machineTag.getNamespace())
            && userAuthService.allowedToModifyDataset(nameFromContext, targetEntityKey))) {
      machineTag.setCreatedBy(nameFromContext);
      return addMachineTag(targetEntityKey, machineTag);
    } else {
      throw new WebApplicationException(HttpStatus.FORBIDDEN);
    }
  }

  @Override
  public int addMachineTag(
      UUID targetEntityKey, @Validated({PrePersist.class, Default.class}) MachineTag machineTag) {
    return withMyBatis.addMachineTag(machineTagMapper, baseMapper, targetEntityKey, machineTag);
  }

  @Override
  public int addMachineTag(
      @NotNull UUID targetEntityKey,
      @NotNull String namespace,
      @NotNull String name,
      @NotNull String value) {
    MachineTag machineTag = new MachineTag();
    machineTag.setNamespace(namespace);
    machineTag.setName(name);
    machineTag.setValue(value);
    return addMachineTag(targetEntityKey, machineTag);
  }

  @Override
  public int addMachineTag(
      @NotNull UUID targetEntityKey, @NotNull TagName tagName, @NotNull String value) {
    MachineTag machineTag = MachineTag.newInstance(tagName, value);
    return addMachineTag(targetEntityKey, machineTag);
  }

  /**
   * The webservice method to delete a machine tag. Ensures that the caller is authorized to perform
   * the action by looking at the namespace.
   */
  @SuppressWarnings("unchecked")
  public void deleteMachineTagByMachineTagKey(
      UUID targetEntityKey, int machineTagKey, Authentication authentication) {
    final String nameFromContext = authentication != null ? authentication.getName() : null;

    List<MachineTag> machineTags = baseMapper.listMachineTags(targetEntityKey);
    Optional<MachineTag> optMachineTag =
        machineTags.stream().filter(m -> m.getKey() == machineTagKey).findFirst();

    if (optMachineTag.isPresent()) {
      MachineTag machineTag = optMachineTag.get();

      if (SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_ADMIN_ROLE)
          || userAuthService.allowedToModifyNamespace(nameFromContext, machineTag.getNamespace())
          || (SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_EDITOR_ROLE)
              && TagNamespace.GBIF_DEFAULT_TERM.getNamespace().equals(machineTag.getNamespace())
              && userAuthService.allowedToModifyDataset(nameFromContext, targetEntityKey))) {
        deleteMachineTag(targetEntityKey, machineTagKey);

      } else {
        throw new WebApplicationException(HttpStatus.FORBIDDEN);
      }
    } else {
      throw new WebApplicationException(HttpStatus.NOT_FOUND);
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
    baseMapper.deleteMachineTag(targetEntityKey, machineTagKey);
  }

  /**
   * The webservice method to delete all machine tag in a namespace. Ensures that the caller is
   * authorized to perform the action by looking at the namespace.
   */
  public void deleteMachineTagsByNamespace(
      UUID targetEntityKey, String namespace, Authentication authentication) {
    final String nameFromContext = authentication != null ? authentication.getName() : null;

    if (!SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_ADMIN_ROLE)
        && !userAuthService.allowedToModifyNamespace(nameFromContext, namespace)) {
      throw new WebApplicationException(HttpStatus.FORBIDDEN);
    }
    deleteMachineTags(targetEntityKey, namespace);
  }

  /**
   * It was added because of an ambiguity problem. (Spring can't distinguish
   * {key}/machineTag/{namespace} and {key}/machineTag/{machineTagKey:[0-9]+})
   */
  @DeleteMapping(value = "{key}/machineTag/{parameter}", consumes = MediaType.ALL_VALUE)
  public void deleteMachineTags(
      @PathVariable("key") UUID targetEntityKey,
      @PathVariable String parameter,
      Authentication authentication) {
    if (Pattern.compile("[0-9]+").matcher(parameter).matches()) {
      deleteMachineTagByMachineTagKey(targetEntityKey, Integer.parseInt(parameter), authentication);
    } else {
      deleteMachineTagsByNamespace(targetEntityKey, parameter, authentication);
    }
  }

  @Override
  public void deleteMachineTags(@NotNull UUID targetEntityKey, @NotNull TagNamespace tagNamespace) {
    deleteMachineTags(targetEntityKey, tagNamespace.getNamespace());
  }

  @Override
  public void deleteMachineTags(@NotNull UUID targetEntityKey, @NotNull String namespace) {
    baseMapper.deleteMachineTags(targetEntityKey, namespace, null);
  }

  /**
   * The webservice method to delete all machine tag of a particular name in a namespace. Ensures
   * that the caller is authorized to perform the action by looking at the namespace.
   */
  @DeleteMapping(value = "{key}/machineTag/{namespace}/{name}", consumes = MediaType.ALL_VALUE)
  public void deleteMachineTags(
      @PathVariable("key") UUID targetEntityKey,
      @PathVariable String namespace,
      @PathVariable String name,
      Authentication authentication) {
    final String nameFromContext = authentication != null ? authentication.getName() : null;

    if (!SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_ADMIN_ROLE)
        && !userAuthService.allowedToModifyNamespace(nameFromContext, namespace)) {
      throw new WebApplicationException(HttpStatus.FORBIDDEN);
    }
    deleteMachineTags(targetEntityKey, namespace, name);
  }

  @Override
  public void deleteMachineTags(@NotNull UUID targetEntityKey, @NotNull TagName tagName) {
    deleteMachineTags(targetEntityKey, tagName.getNamespace().getNamespace(), tagName.getName());
  }

  @Override
  public void deleteMachineTags(
      @NotNull UUID targetEntityKey, @NotNull String namespace, @NotNull String name) {
    baseMapper.deleteMachineTags(targetEntityKey, namespace, name);
  }

  @SuppressWarnings("unchecked")
  @GetMapping("{key}/machineTag")
  @Override
  public List<MachineTag> listMachineTags(@PathVariable("key") UUID targetEntityKey) {
    return baseMapper.listMachineTags(targetEntityKey);
  }

  public PagingResponse<T> listByMachineTag(
      String namespace, @Nullable String name, @Nullable String value, Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return withMyBatis.listByMachineTag(baseMapper, namespace, name, value, page);
  }
}
