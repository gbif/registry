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

import org.gbif.api.annotation.Trim;
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
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.TagName;
import org.gbif.api.vocabulary.TagNamespace;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.collections.ChangedCollectionEntityComponentEvent;
import org.gbif.registry.events.collections.DeleteCollectionEntityEvent;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.BaseMapper;
import org.gbif.registry.security.EditorAuthorizationService;
import org.gbif.registry.security.SecurityContextCheck;
import org.gbif.ws.NotFoundException;
import org.gbif.ws.WebApplicationException;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_EDITOR_ROLE;

/** Base class to implement the CRUD methods of a {@link CollectionEntity}. */
@Validated
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

  public void preCreate(T entity) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (!isAllowedToEditEntity(authentication, entity)) {
      throw new WebApplicationException(
          "User is not allowed to modify GrSciColl entity", HttpStatus.FORBIDDEN);
    }

    final String username = authentication.getName();
    entity.setCreatedBy(username);
    entity.setModifiedBy(username);
  }

  @DeleteMapping("{key}")
  @Transactional
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Override
  public void delete(@PathVariable UUID key) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    T entityToDelete = get(key);
    checkArgument(entityToDelete != null, "Entity to delete doesn't exist");

    if (!isAllowedToEditEntity(authentication, entityToDelete)) {
      throw new WebApplicationException(
          "User is not allowed to modify GrSciColl entity", HttpStatus.FORBIDDEN);
    }

    entityToDelete.setModifiedBy(authentication.getName());
    update(entityToDelete);

    baseMapper.delete(key);
    eventManager.post(DeleteCollectionEntityEvent.newInstance(entityToDelete, objectClass));
  }

  @Nullable
  @Override
  public T get(@PathVariable UUID key) {
    return baseMapper.get(key);
  }

  public void preUpdate(T entity) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (!isAllowedToEditEntity(authentication, entity)) {
      throw new WebApplicationException(
          "User is not allowed to modify GrSciColl entity", HttpStatus.FORBIDDEN);
    }

    entity.setModifiedBy(authentication.getName());
  }

  @PostMapping(value = "{key}/identifier", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Override
  public int addIdentifier(
      @PathVariable("key") UUID entityKey, @RequestBody Identifier identifier) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    // only admins can add IH identifiers
    if (identifier.getType() == IdentifierType.IH_IRN
        && !SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_ADMIN_ROLE)) {
      throw new WebApplicationException(
          "User is not allowed to modify GrSciColl entity", HttpStatus.FORBIDDEN);
    }

    identifier.setCreatedBy(authentication.getName());
    int identifierKey =
        withMyBatis.addIdentifier(identifierMapper, baseMapper, entityKey, identifier);
    eventManager.post(
        ChangedCollectionEntityComponentEvent.newInstance(
            entityKey, objectClass, Identifier.class));
    return identifierKey;
  }

  @DeleteMapping("{key}/identifier/{identifierKey}")
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Transactional
  @Override
  public void deleteIdentifier(
      @PathVariable("key") UUID entityKey, @PathVariable int identifierKey) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    // check if the user has permissions to delete the identifier. Only admins can delete IH
    // identifiers.
    List<Identifier> identifiers = baseMapper.listIdentifiers(entityKey);
    Optional<Identifier> identifierToDelete =
        identifiers.stream().filter(i -> i.getKey().equals(identifierKey)).findFirst();

    if (identifierToDelete.isPresent()
        && identifierToDelete.get().getType() == IdentifierType.IH_IRN
        && !SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_ADMIN_ROLE)) {
      throw new WebApplicationException(
          "User is not allowed to modify GrSciColl entity", HttpStatus.FORBIDDEN);
    }

    baseMapper.deleteIdentifier(entityKey, identifierKey);
    eventManager.post(
        ChangedCollectionEntityComponentEvent.newInstance(
            entityKey, objectClass, Identifier.class));
  }

  @GetMapping("{key}/identifier")
  @Nullable
  @Override
  public List<Identifier> listIdentifiers(@PathVariable UUID key) {
    return baseMapper.listIdentifiers(key);
  }

  @PostMapping(value = "{key}/tag", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Override
  public int addTag(@PathVariable("key") UUID entityKey, @RequestBody Tag tag) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    tag.setCreatedBy(authentication.getName());
    int tagKey = withMyBatis.addTag(tagMapper, baseMapper, entityKey, tag);
    eventManager.post(
        ChangedCollectionEntityComponentEvent.newInstance(entityKey, objectClass, Tag.class));
    return tagKey;
  }

  @Override
  public int addTag(UUID key, String value) {
    Tag tag = new Tag();
    tag.setValue(value);
    return addTag(key, tag);
  }

  @DeleteMapping("{key}/tag/{tagKey}")
  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_EDITOR_ROLE})
  @Transactional
  @Override
  public void deleteTag(@PathVariable("key") UUID entityKey, @PathVariable int tagKey) {
    baseMapper.deleteTag(entityKey, tagKey);
    eventManager.post(
        ChangedCollectionEntityComponentEvent.newInstance(entityKey, objectClass, Tag.class));
  }

  @GetMapping("{key}/tag")
  @Nullable
  @Override
  public List<Tag> listTags(
      @PathVariable("key") UUID key,
      @RequestParam(value = "owner", required = false) String owner) {
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
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Transactional
  @Override
  public int addMachineTag(
      @PathVariable("key") UUID targetEntityKey, @RequestBody @Trim MachineTag machineTag) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String nameFromContext = authentication != null ? authentication.getName() : null;

    if (SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_ADMIN_ROLE)
        || userAuthService.allowedToModifyNamespace(nameFromContext, machineTag.getNamespace())
        || (SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_EDITOR_ROLE)
            && TagNamespace.GBIF_DEFAULT_TERM.getNamespace().equals(machineTag.getNamespace()))) {
      machineTag.setCreatedBy(nameFromContext);
      return withMyBatis.addMachineTag(machineTagMapper, baseMapper, targetEntityKey, machineTag);
    } else {
      throw new WebApplicationException(
          "User is not allowed to modify collection " + nameFromContext, HttpStatus.FORBIDDEN);
    }
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
  @DeleteMapping("{key}/machineTag/{machineTagKey:[0-9]+}")
  public void deleteMachineTagByMachineTagKey(
      @PathVariable("key") UUID targetEntityKey,
      @PathVariable("machineTagKey") int machineTagKey,
      Authentication authentication) {
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
        throw new WebApplicationException(
            "User is not allowed to modify collection " + nameFromContext, HttpStatus.FORBIDDEN);
      }
    } else {
      throw new NotFoundException("Machine tag was not found", URI.create("/"));
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
  @DeleteMapping("{key}/machineTag/{namespace:.*[^0-9]+.*}")
  public void deleteMachineTagsByNamespace(
      @PathVariable("key") UUID targetEntityKey,
      @PathVariable("namespace") String namespace,
      Authentication authentication) {
    final String nameFromContext = authentication != null ? authentication.getName() : null;

    if (!SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_ADMIN_ROLE)
        && !userAuthService.allowedToModifyNamespace(nameFromContext, namespace)) {
      throw new WebApplicationException(
          "User is not allowed to modify collection " + nameFromContext, HttpStatus.FORBIDDEN);
    }
    deleteMachineTags(targetEntityKey, namespace);
  }

  @Override
  public void deleteMachineTags(UUID targetEntityKey, TagNamespace tagNamespace) {
    deleteMachineTags(targetEntityKey, tagNamespace.getNamespace());
  }

  @Override
  public void deleteMachineTags(UUID targetEntityKey, String namespace) {
    baseMapper.deleteMachineTags(targetEntityKey, namespace, null);
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

    if (!SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_ADMIN_ROLE)
        && !userAuthService.allowedToModifyNamespace(nameFromContext, namespace)) {
      throw new WebApplicationException(
          "User is not allowed to modify collection " + nameFromContext, HttpStatus.FORBIDDEN);
    }
    deleteMachineTags(targetEntityKey, namespace, name);
  }

  @Override
  public void deleteMachineTags(UUID targetEntityKey, TagName tagName) {
    deleteMachineTags(targetEntityKey, tagName.getNamespace().getNamespace(), tagName.getName());
  }

  @Override
  public void deleteMachineTags(UUID targetEntityKey, String namespace, String name) {
    baseMapper.deleteMachineTags(targetEntityKey, namespace, name);
  }

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

  /** Only admins can edit IH entities. */
  protected boolean isAllowedToEditEntity(Authentication authentication, T entity) {
    if (SecurityContextCheck.checkUserInRole(authentication, GRSCICOLL_ADMIN_ROLE)) {
      return true;
    }

    boolean isIhEntity =
        entity.getIdentifiers().stream().anyMatch(i -> i.getType() == IdentifierType.IH_IRN);
    return !isIhEntity;
  }
}
