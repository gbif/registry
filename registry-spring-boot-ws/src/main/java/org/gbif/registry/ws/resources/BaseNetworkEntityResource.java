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
import org.gbif.registry.ws.Trim;
import org.gbif.registry.ws.security.EditorAuthorizationService;
import org.gbif.registry.ws.security.SecurityContextCheck;
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
  // TODO: 2019-08-20 mb wrap it
  private final EventBus eventBus;
  private final EditorAuthorizationService userAuthService;
  private final WithMyBatis withMyBatis;
  private final Class<T> objectClass;

  public BaseNetworkEntityResource(
      BaseNetworkEntityMapper<T> mapper,
      CommentMapper commentMapper,
      EventBus eventBus,
      EditorAuthorizationService userAuthService,
      WithMyBatis withMyBatis,
      Class<T> objectClass) {
    this.mapper = mapper;
    this.commentMapper = commentMapper;
    this.eventBus = eventBus;
    this.userAuthService = userAuthService;
    this.withMyBatis = withMyBatis;
    this.objectClass = objectClass;
  }

  // TODO: 2019-08-26 add APPLICATION_JAVASCRIPT type for all

  /**
   * This method ensures that the caller is authorized to perform the action and then adds the server
   * controlled fields for createdBy and modifiedBy. It then creates the entity.
   *
   * @param entity entity that extends NetworkEntity
   * @return key of entity created
   */
  @Override
  @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  // TODO: 2019-08-20 bval Validate -> use something else instead?
//  @Validate(groups = {PrePersist.class, Default.class})
  public UUID create(@NotNull @Valid @Trim T entity) {
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
  @Override
  public void delete(@NotNull @PathVariable("key") UUID key) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final UserDetails principal = (UserDetails) authentication.getPrincipal();

    // the following lines allow to set the "modifiedBy" to the user who actually deletes the entity.
    // the api delete(UUID) should be changed eventually
    T objectToDelete = get(key);
    objectToDelete.setModifiedBy(principal.getUsername());
    withMyBatis.update(mapper, objectToDelete);

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

  @Override
  public PagingResponse<T> listByMachineTag(String s, @Nullable String s1, @Nullable String s2, @Nullable Pageable pageable) {
    throw new UnsupportedOperationException("not implemented yet");
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

  @Override
  public void deleteComment(@NotNull UUID uuid, int i) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public List<Comment> listComments(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("not implemented yet");
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
  public int addMachineTag(@NotNull UUID uuid, @NotNull MachineTag machineTag) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public int addMachineTag(@NotNull UUID uuid, @NotNull TagName tagName, @NotNull String s) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public int addMachineTag(@NotNull UUID uuid, @NotNull String s, @NotNull String s1, @NotNull String s2) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void deleteMachineTag(@NotNull UUID uuid, int i) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull TagNamespace tagNamespace) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull String s) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull TagName tagName) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull String s, @NotNull String s1) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public List<MachineTag> listMachineTags(@NotNull UUID uuid) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public int addTag(@NotNull UUID uuid, @NotNull String s) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public int addTag(@NotNull UUID uuid, @NotNull Tag tag) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public void deleteTag(@NotNull UUID uuid, int i) {
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
