package org.gbif.registry.ws.resources;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.service.registry.NetworkEntityService;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.TagName;
import org.gbif.api.vocabulary.TagNamespace;
import org.gbif.registry.events.ChangedComponentEvent;
import org.gbif.registry.events.CreateEvent;
import org.gbif.registry.events.DeleteEvent;
import org.gbif.registry.events.UpdateEvent;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.BaseNetworkEntityMapper;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.ws.Trim;
import org.gbif.registry.ws.security.EditorAuthorizationService;
import org.gbif.registry.ws.security.SecurityContextCheck;
import org.gbif.registry.ws.security.UserRoles;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.server.interceptor.NullToNotFound;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.APP_ROLE;
import static org.gbif.registry.ws.security.UserRoles.EDITOR_ROLE;

/**
 * Provides a skeleton implementation of the following.
 * <ul>
 * <li>Base CRUD operations for a network entity</li>
 * <li>Comment operations</li>
 * <li>Contact operations (in addition to BaseNetworkEntityResource)</li>
 * <li>Endpoint operations (in addition to BaseNetworkEntityResource)</li>
 * <li>Identifier operations (in addition to BaseNetworkEntityResource2)</li>
 * <li>MachineTag operations</li>
 * <li>Tag operations</li>
 * </ul>
 *
 * @param <T> The type of resource that is under CRUD
 */
public class BaseNetworkEntityResource<T extends NetworkEntity> implements NetworkEntityService<T> {

  private final BaseNetworkEntityMapper<T> mapper;
  private final CommentMapper commentMapper;
  private final MachineTagMapper machineTagMapper;
  private final TagMapper tagMapper;
  // TODO: 2019-08-20 mb wrap it
  private final EventBus eventBus;
  private final EditorAuthorizationService userAuthService;
  private final WithMyBatis withMyBatis;
  private final Class<T> objectClass;

  public BaseNetworkEntityResource(
      BaseNetworkEntityMapper<T> mapper,
      CommentMapper commentMapper,
      MachineTagMapper machineTagMapper,
      TagMapper tagMapper,
      EventBus eventBus,
      EditorAuthorizationService userAuthService,
      WithMyBatis withMyBatis,
      Class<T> objectClass) {
    this.mapper = mapper;
    this.commentMapper = commentMapper;
    this.machineTagMapper = machineTagMapper;
    this.tagMapper = tagMapper;
    this.eventBus = eventBus;
    this.userAuthService = userAuthService;
    this.withMyBatis = withMyBatis;
    this.objectClass = objectClass;
  }

  // TODO: 2019-08-26 validation
  // TODO: 2019-08-26 add APPLICATION_JAVASCRIPT type for all

  /**
   * This method ensures that the caller is authorized to perform the action and then adds the server
   * controlled fields for createdBy and modifiedBy. It then creates the entity.
   *
   * @param entity entity that extends NetworkEntity
   * @return key of entity created
   */
  @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public UUID createBase(@NotNull @Trim T entity) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final UserDetails principal = (UserDetails) authentication.getPrincipal();
    // if not admin or app, verify rights
    if (!SecurityContextCheck.checkUserInRole(authentication, ADMIN_ROLE, APP_ROLE)) {
      boolean allowed = false;
      for (UUID entityKeyToBeAssessed : owningEntityKeys(entity)) {
        if (entityKeyToBeAssessed == null) {
          throw new WebApplicationException(HttpStatus.FORBIDDEN);
        }
        if (userAuthService.allowedToModifyEntity(principal, entityKeyToBeAssessed)) {
          allowed = true;
          break;
        }
      }
      if (!allowed) {
        throw new WebApplicationException(HttpStatus.FORBIDDEN);
      }
    }
    entity.setCreatedBy(principal.getUsername());
    entity.setModifiedBy(principal.getUsername());

    return create(entity);
  }

  //  @Validate(groups = {PrePersist.class, Default.class})
  @Override
  public UUID create(@Valid T entity) {
    withMyBatis.create(mapper, entity);
    eventBus.post(CreateEvent.newInstance(entity, objectClass));
    return entity.getKey();
  }

  /**
   * This method ensures that the caller is authorized to perform the action, and then deletes the entity.
   * </br>
   * Relax content-type to wildcard to allow angularjs.
   *
   * @param key key of entity to delete
   */
  @DeleteMapping(value = "{key}", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Transactional
  public void deleteBase(@NotNull @PathVariable("key") UUID key) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final UserDetails principal = (UserDetails) authentication.getPrincipal();

    // the following lines allow to set the "modifiedBy" to the user who actually deletes the entity.
    // the api delete(UUID) should be changed eventually
    T objectToDelete = get(key);
    objectToDelete.setModifiedBy(principal.getUsername());
    withMyBatis.update(mapper, objectToDelete);

    delete(key);
  }

  @Override
  public void delete(UUID key) {
    T objectToDelete = get(key);
    withMyBatis.delete(mapper, key);
    eventBus.post(DeleteEvent.newInstance(objectToDelete, objectClass));
  }

  //  @Validate(groups = {PostPersist.class, Default.class})
  @NullToNotFound
  @Nullable
  @GetMapping(value = "{key}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public T get(@NotNull @PathVariable("key") UUID key) {
    return withMyBatis.get(mapper, key);
  }

  // we do a post not get cause we expect large numbers of keys to be sent
  @PostMapping("titles")
  @Override
  public Map<UUID, String> getTitles(Collection<UUID> keys) {
    Map<UUID, String> titles = Maps.newHashMap();
    for (UUID key : keys) {
      titles.put(key, mapper.title(key));
    }
    return titles;
  }

  @Override
  public PagingResponse<T> list(@Nullable Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return withMyBatis.list(mapper, page);
  }

  @Override
  public PagingResponse<T> search(String query, @Nullable Pageable page) {
    page = page == null ? new PagingRequest() : page;
    // trim and handle null from given input
    String q = query != null ? Strings.emptyToNull(CharMatcher.WHITESPACE.trimFrom(query)) : query;
    return withMyBatis.search(mapper, q, page);
  }

  @Override
  public PagingResponse<T> listByIdentifier(IdentifierType type, String identifier, @Nullable Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return withMyBatis.listByIdentifier(mapper, type, identifier, page);
  }

  @Override
  public PagingResponse<T> listByIdentifier(String identifier, @Nullable Pageable page) {
    return listByIdentifier(null, identifier, page);
  }

  /**
   * This method ensures that the path variable for the key matches the entity's key, ensures that the caller is
   * authorized to perform the action and then adds the server controlled field modifiedBy.
   *
   * @param key    key of entity to update
   * @param entity entity that extends NetworkEntity
   */
  @PutMapping(value = "{key}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public void update(@PathVariable("key") UUID key, @NotNull @Trim T entity) {
    checkArgument(key.equals(entity.getKey()), "Provided entity must have the same key as the resource URL");
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final UserDetails principal = (UserDetails) authentication.getPrincipal();
    entity.setModifiedBy(principal.getUsername());
    update(entity);
  }

  //  @Validate(groups = {PostPersist.class, Default.class})
  @Override
  public void update(@Valid T entity) {
    T oldEntity = get(entity.getKey());
    withMyBatis.update(mapper, entity);
    // get complete entity with components populated, so subscribers of UpdateEvent can compare new and old entities
    T newEntity = get(entity.getKey());
    eventBus.post(UpdateEvent.newInstance(newEntity, oldEntity, objectClass));
  }

  /**
   * This method ensures that the caller is authorized to perform the action and then adds the server
   * controlled fields for createdBy and modifiedBy.
   *
   * @param targetEntityKey key of target entity to add comment to
   * @param comment         Comment to add
   * @return key of Comment created
   */
  @PostMapping(value = "{key}/comment", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE, APP_ROLE})
  public int addCommentBase(@NotNull @PathVariable("key") UUID targetEntityKey, @NotNull @Trim Comment comment) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final UserDetails principal = (UserDetails) authentication.getPrincipal();
    comment.setCreatedBy(principal.getUsername());
    comment.setModifiedBy(principal.getUsername());
    return addComment(targetEntityKey, comment);
  }

  //  @Validate(groups = {PrePersist.class, Default.class})
  @Override
  public int addComment(UUID targetEntityKey, @Valid Comment comment) {
    int key = withMyBatis.addComment(commentMapper, mapper, targetEntityKey, comment);
    eventBus.post(ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Comment.class));
    return key;
  }

  /**
   * This method ensures that the caller is authorized to perform the action, and then deletes the Comment.
   *
   * @param targetEntityKey key of target entity to delete comment from
   * @param commentKey      key of Comment to delete
   */
  @DeleteMapping(value = "{key}/comment/{commentKey}", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public void deleteComment(@NotNull @PathVariable("key") UUID targetEntityKey, @PathVariable("commentKey") int commentKey) {
    withMyBatis.deleteComment(mapper, targetEntityKey, commentKey);
    eventBus.post(ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Comment.class));
  }

  @GetMapping(value = "{key}/comment", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public List<Comment> listComments(@NotNull @PathVariable("key") UUID targetEntityKey) {
    return withMyBatis.listComments(mapper, targetEntityKey);
  }

  /**
   * Adding machineTags is not restricted to certain roles on the method level, but instead handled
   * controlled field for createdBy.
   *
   * @param targetEntityKey key of target entity to add MachineTag to
   * @param machineTag      MachineTag to add
   * @return key of MachineTag created
   */
  @PostMapping(value = "{key}/machineTag", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Transactional
  public int addMachineTagBase(@PathVariable("key") UUID targetEntityKey, @RequestBody @NotNull @Trim MachineTag machineTag) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final UserDetails principal = (UserDetails) authentication.getPrincipal();

    if (!SecurityContextCheck.checkUserInRole(authentication, ADMIN_ROLE)
        && !userAuthService.allowedToModifyNamespace(principal, machineTag.getNamespace())) {
      throw new WebApplicationException(HttpStatus.FORBIDDEN);
    }

    machineTag.setCreatedBy(principal.getUsername());
    return addMachineTag(targetEntityKey, machineTag);
  }

  //  @Validate(groups = {PrePersist.class, Default.class})
  @Override
  public int addMachineTag(UUID targetEntityKey, @Valid MachineTag machineTag) {
    return withMyBatis.addMachineTag(machineTagMapper, mapper, targetEntityKey, machineTag);
  }

  @Override
  public int addMachineTag(@NotNull UUID targetEntityKey, @NotNull String namespace, @NotNull String name, @NotNull String value) {
    MachineTag machineTag = new MachineTag();
    machineTag.setNamespace(namespace);
    machineTag.setName(name);
    machineTag.setValue(value);
    return addMachineTag(targetEntityKey, machineTag);
  }

  @Override
  public int addMachineTag(@NotNull UUID targetEntityKey, @NotNull TagName tagName, @NotNull String value) {
    MachineTag machineTag = MachineTag.newInstance(tagName, value);
    return addMachineTag(targetEntityKey, machineTag);
  }

  /**
   * The webservice method to delete a machine tag.
   * Ensures that the caller is authorized to perform the action by looking at the namespace.
   */
  // TODO: 2019-08-26 test machineTagKey should be {machineTagKey: [0-9]+}
  @DeleteMapping(value = "{key}/machineTag/{machineTagKey: [0-9]+} ", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public void deleteMachineTagBase(@PathVariable("key") UUID targetEntityKey, @PathVariable("machineTagKey") int machineTagKey) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final UserDetails principal = (UserDetails) authentication.getPrincipal();

    if (!SecurityContextCheck.checkUserInRole(authentication, UserRoles.ADMIN_ROLE)
        && !userAuthService.allowedToDeleteMachineTag(principal, machineTagKey)) {
      throw new WebApplicationException(HttpStatus.FORBIDDEN);
    }
    deleteMachineTag(targetEntityKey, machineTagKey);
  }

  /**
   * Deletes the MachineTag according to interface without security restrictions.
   *
   * @param targetEntityKey key of target entity to delete MachineTag from
   * @param machineTagKey   key of MachineTag to delete
   */
  @Override
  public void deleteMachineTag(UUID targetEntityKey, int machineTagKey) {
    withMyBatis.deleteMachineTag(mapper, targetEntityKey, machineTagKey);
  }

  /**
   * The webservice method to delete all machine tag in a namespace.
   * Ensures that the caller is authorized to perform the action by looking at the namespace.
   */
  @DeleteMapping(value = "{key}/machineTag/{namespace}", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public void deleteMachineTagsBase(@PathVariable("key") UUID targetEntityKey, @PathVariable("namespace") String namespace) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final UserDetails principal = (UserDetails) authentication.getPrincipal();
    if (!SecurityContextCheck.checkUserInRole(authentication, UserRoles.ADMIN_ROLE)
        && !userAuthService.allowedToModifyNamespace(principal, namespace)) {
      throw new WebApplicationException(HttpStatus.FORBIDDEN);
    }
    deleteMachineTags(targetEntityKey, namespace);
  }

  @Override
  public void deleteMachineTags(@NotNull UUID targetEntityKey, @NotNull TagNamespace tagNamespace) {
    deleteMachineTags(targetEntityKey, tagNamespace.getNamespace());
  }

  @Override
  public void deleteMachineTags(@NotNull UUID targetEntityKey, @NotNull String namespace) {
    withMyBatis.deleteMachineTags(mapper, targetEntityKey, namespace, null);
  }

  /**
   * The webservice method to delete all machine tag of a particular name in a namespace.
   * Ensures that the caller is authorized to perform the action by looking at the namespace.
   */
  @DeleteMapping(value = "{key}/machineTag/{namespace}/{name}", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public void deleteMachineTagsBase(@PathVariable("key") UUID targetEntityKey, @PathVariable("namespace") String namespace,
                                    @PathVariable("name") String name) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final UserDetails principal = (UserDetails) authentication.getPrincipal();

    if (!SecurityContextCheck.checkUserInRole(authentication, UserRoles.ADMIN_ROLE)
        && !userAuthService.allowedToModifyNamespace(principal, namespace)) {
      throw new WebApplicationException(HttpStatus.FORBIDDEN);
    }
    deleteMachineTags(targetEntityKey, namespace, name);
  }

  @Override
  public void deleteMachineTags(@NotNull UUID targetEntityKey, @NotNull TagName tagName) {
    deleteMachineTags(targetEntityKey, tagName.getNamespace().getNamespace(), tagName.getName());
  }

  @Override
  public void deleteMachineTags(@NotNull UUID targetEntityKey, @NotNull String namespace, @NotNull String name) {
    withMyBatis.deleteMachineTags(mapper, targetEntityKey, namespace, name);
  }

  @GetMapping(value = "{key}/machineTag", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public List<MachineTag> listMachineTags(@PathVariable("key") UUID targetEntityKey) {
    return withMyBatis.listMachineTags(mapper, targetEntityKey);
  }

  @Override
  public PagingResponse<T> listByMachineTag(String namespace, @Nullable String name, @Nullable String value,
                                            @Nullable Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return withMyBatis.listByMachineTag(mapper, namespace, name, value, page);
  }

  /**
   * This method ensures that the caller is authorized to perform the action and then adds the server
   * controlled fields for createdBy.
   *
   * @param targetEntityKey key of target entity to add Tag to
   * @param tag             Tag to add
   * @return key of Tag created
   */
  @PostMapping(value = "{key}/tag", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public int addTagBase(@PathVariable("key") UUID targetEntityKey, @NotNull Tag tag) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final UserDetails principal = (UserDetails) authentication.getPrincipal();
    tag.setCreatedBy(principal.getUsername());
    return addTag(targetEntityKey, tag);
  }

  @Override
  public int addTag(@NotNull UUID targetEntityKey, @NotNull String value) {
    Tag tag = new Tag();
    tag.setValue(value);
    return addTag(targetEntityKey, tag);
  }

  //  @Validate(groups = {PrePersist.class, Default.class})
  @Override
  public int addTag(UUID targetEntityKey, @Valid Tag tag) {
    int key = withMyBatis.addTag(tagMapper, mapper, targetEntityKey, tag);
    eventBus.post(ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Tag.class));
    return key;
  }

  /**
   * This method ensures that the caller is authorized to perform the action, and then deletes the Tag.
   *
   * @param targetEntityKey key of target entity to delete Tag from
   * @param tagKey          key of Tag to delete
   */
  @DeleteMapping(value = "{key}/tag/{tagKey}", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public void deleteTag(@PathVariable("key") UUID targetEntityKey, @PathVariable("tagKey") int tagKey) {
    withMyBatis.deleteTag(mapper, targetEntityKey, tagKey);
    eventBus.post(ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Tag.class));
  }

  @Override
  public int addContact(@NotNull UUID uuid, @NotNull Contact contact) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void deleteContact(@NotNull UUID uuid, int i) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public List<Contact> listContacts(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void updateContact(@NotNull UUID uuid, @NotNull Contact contact) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public int addEndpoint(@NotNull UUID uuid, @NotNull Endpoint endpoint) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void deleteEndpoint(@NotNull UUID uuid, int i) {

  }

  @Override
  public List<Endpoint> listEndpoints(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public int addIdentifier(@NotNull UUID uuid, @NotNull Identifier identifier) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void deleteIdentifier(@NotNull UUID uuid, int i) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public List<Identifier> listIdentifiers(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public List<Tag> listTags(@NotNull UUID uuid, @Nullable String s) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  /**
   * Override this method to extract the entity key that governs security rights for creating.
   * If null is returned only admins are allowed to create new entities which is the default.
   */
  protected List<UUID> owningEntityKeys(@NotNull T entity) {
    return new ArrayList<>();
  }
}
