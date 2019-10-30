package org.gbif.registry.ws.resources;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
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
import org.gbif.registry.ws.security.EditorAuthorizationService;
import org.gbif.registry.ws.security.SecurityContextCheck;
import org.gbif.registry.ws.security.UserRoles;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.annotation.NullToNotFound;
import org.gbif.ws.annotation.Trim;
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

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

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
@RequestMapping(path = "/",
  consumes = MediaType.APPLICATION_JSON_VALUE,
  produces = MediaType.APPLICATION_JSON_VALUE)
public class BaseNetworkEntityResource<T extends NetworkEntity> implements NetworkEntityService<T> {

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
    CommentMapper commentMapper,
    ContactMapper contactMapper,
    EndpointMapper endpointMapper,
    IdentifierMapper identifierMapper,
    MachineTagMapper machineTagMapper,
    TagMapper tagMapper,
    Class<T> objectClass,
    EventManager eventManager,
    EditorAuthorizationService userAuthService,
    WithMyBatis withMyBatis) {
    this.mapper = mapper;
    this.commentMapper = commentMapper;
    this.machineTagMapper = machineTagMapper;
    this.tagMapper = tagMapper;
    this.contactMapper = contactMapper;
    this.endpointMapper = endpointMapper;
    this.identifierMapper = identifierMapper;
    this.objectClass = objectClass;
    this.eventManager = eventManager;
    this.userAuthService = userAuthService;
    this.withMyBatis = withMyBatis;
  }

  /**
   * This method ensures that the caller is authorized to perform the action and then adds the server
   * controlled fields for createdBy and modifiedBy. It then creates the entity.
   *
   * @param entity entity that extends NetworkEntity
   * @return key of entity created
   */
  @PostMapping
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public UUID create(@RequestBody @NotNull @Trim @Validated({PrePersist.class, Default.class}) T entity, Authentication authentication) {
    final String nameFromContext = authentication != null ? authentication.getName() : null;
    // if not admin or app, verify rights
    if (!SecurityContextCheck.checkUserInRole(authentication, ADMIN_ROLE, APP_ROLE)) {
      boolean allowed = false;
      for (UUID entityKeyToBeAssessed : owningEntityKeys(entity)) {
        if (entityKeyToBeAssessed == null) {
          throw new WebApplicationException(HttpStatus.FORBIDDEN);
        }
        if (userAuthService.allowedToModifyEntity(nameFromContext, entityKeyToBeAssessed)) {
          allowed = true;
          break;
        }
      }
      if (!allowed) {
        throw new WebApplicationException(HttpStatus.FORBIDDEN);
      }
    }
    entity.setCreatedBy(nameFromContext);
    entity.setModifiedBy(nameFromContext);

    return create(entity);
  }

  @Override
  public UUID create(@Validated({PrePersist.class, Default.class}) T entity) {
    withMyBatis.create(mapper, entity);
    eventManager.post(CreateEvent.newInstance(entity, objectClass));
    return entity.getKey();
  }

  // TODO: 16/10/2019 analyze and maybe merge duplicated methods like delete(key,auth) and delete(key)

  /**
   * This method ensures that the caller is authorized to perform the action, and then deletes the entity.
   * </br>
   * Relax content-type to wildcard to allow angularjs.
   *
   * @param key key of entity to delete
   */
  @DeleteMapping(value = "{key}", consumes = MediaType.ALL_VALUE)
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Transactional
  public void delete(@NotNull @PathVariable UUID key, Authentication authentication) {
    // the following lines allow to set the "modifiedBy" to the user who actually deletes the entity.
    // the api delete(UUID) should be changed eventually
    T objectToDelete = get(key);

    if (objectToDelete != null) {
      objectToDelete.setModifiedBy(authentication.getName());
      withMyBatis.update(mapper, objectToDelete);
      delete(key);
    }
  }

  @Override
  public void delete(UUID key) {
    T objectToDelete = get(key);
    withMyBatis.delete(mapper, key);
    eventManager.post(DeleteEvent.newInstance(objectToDelete, objectClass));
  }

  @NullToNotFound
  @Nullable
  @GetMapping(value = "{key}")
  @Override
  public T get(@NotNull @PathVariable UUID key) {
    return withMyBatis.get(mapper, key);
  }

  // we do a post not get cause we expect large numbers of keys to be sent
  @PostMapping("titles")
  @Override
  public Map<UUID, String> getTitles(@RequestBody Collection<UUID> keys) {
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
   * This method ensures that the path variable for the key matches the entity's key, ensures that the caller is
   * authorized to perform the action and then adds the server controlled field modifiedBy.
   *
   * @param key    key of entity to update
   * @param entity entity that extends NetworkEntity
   */
  @PutMapping(value = "{key}")
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public void update(@PathVariable UUID key,
                     @RequestBody @NotNull @Trim @Validated({PostPersist.class, Default.class}) T entity,
                     Authentication authentication) {
    checkArgument(key.equals(entity.getKey()), "Provided entity must have the same key as the resource URL");
    entity.setModifiedBy(authentication.getName());
    update(entity);
  }

  @Override
  public void update(@Validated({PostPersist.class, Default.class}) T entity) {
    T oldEntity = get(entity.getKey());
    withMyBatis.update(mapper, entity);
    // get complete entity with components populated, so subscribers of UpdateEvent can compare new and old entities
    T newEntity = get(entity.getKey());
    eventManager.post(UpdateEvent.newInstance(newEntity, oldEntity, objectClass));
  }

  /**
   * This method ensures that the caller is authorized to perform the action and then adds the server
   * controlled fields for createdBy and modifiedBy.
   *
   * @param targetEntityKey key of target entity to add comment to
   * @param comment         Comment to add
   * @return key of Comment created
   */
  @PostMapping(value = "{key}/comment")
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE, APP_ROLE})
  public int addComment(@NotNull @PathVariable("key") UUID targetEntityKey, @RequestBody @NotNull @Trim Comment comment,
                        Authentication authentication) {
    comment.setCreatedBy(authentication.getName());
    comment.setModifiedBy(authentication.getName());
    return addComment(targetEntityKey, comment);
  }

  @Override
  public int addComment(UUID targetEntityKey, @Validated({PrePersist.class, Default.class}) Comment comment) {
    int key = withMyBatis.addComment(commentMapper, mapper, targetEntityKey, comment);
    eventManager.post(ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Comment.class));
    return key;
  }

  /**
   * This method ensures that the caller is authorized to perform the action, and then deletes the Comment.
   *
   * @param targetEntityKey key of target entity to delete comment from
   * @param commentKey      key of Comment to delete
   */
  @DeleteMapping(value = "{key}/comment/{commentKey}", consumes = MediaType.ALL_VALUE)
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public void deleteComment(@NotNull @PathVariable("key") UUID targetEntityKey, @PathVariable int commentKey) {
    withMyBatis.deleteComment(mapper, targetEntityKey, commentKey);
    eventManager.post(ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Comment.class));
  }

  @GetMapping(value = "{key}/comment")
  @Override
  public List<Comment> listComments(@NotNull @PathVariable("key") UUID targetEntityKey) {
    return withMyBatis.listComments(mapper, targetEntityKey);
  }

  /**
   * Adding most machineTags is restricted based on the namespace.
   * For some tags, it is restricted based on the editing role as usual.
   *
   * @param targetEntityKey key of target entity to add MachineTag to
   * @param machineTag      MachineTag to add
   * @return key of MachineTag created
   */
  @PostMapping(value = "{key}/machineTag")
  @Trim
  @Transactional
  public int addMachineTag(@PathVariable("key") UUID targetEntityKey, @RequestBody @NotNull @Trim MachineTag machineTag,
                           Authentication authentication) {
    final String nameFromContext = authentication != null ? authentication.getName() : null;

    if (SecurityContextCheck.checkUserInRole(authentication, ADMIN_ROLE)
      || userAuthService.allowedToModifyNamespace(nameFromContext, machineTag.getNamespace())
      || (SecurityContextCheck.checkUserInRole(authentication, EDITOR_ROLE)
      && TagNamespace.GBIF_DEFAULT_TERM.getNamespace().equals(machineTag.getNamespace())
      && userAuthService.allowedToModifyDataset(nameFromContext, targetEntityKey))
    ) {
      machineTag.setCreatedBy(nameFromContext);
      return addMachineTag(targetEntityKey, machineTag);
    } else {
      throw new WebApplicationException(HttpStatus.FORBIDDEN);
    }
  }

  @Override
  public int addMachineTag(UUID targetEntityKey, @Validated({PrePersist.class, Default.class}) MachineTag machineTag) {
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
  public void deleteMachineTagByMachineTagKey(UUID targetEntityKey, int machineTagKey, Authentication authentication) {
    final String nameFromContext = authentication != null ? authentication.getName() : null;

    Optional<MachineTag> optMachineTag = withMyBatis.listMachineTags(mapper, targetEntityKey).stream()
      .filter(m -> m.getKey() == machineTagKey).findFirst();

    if (optMachineTag.isPresent()) {
      MachineTag machineTag = optMachineTag.get();

      if (SecurityContextCheck.checkUserInRole(authentication, ADMIN_ROLE)
        || userAuthService.allowedToModifyNamespace(nameFromContext, machineTag.getNamespace())
        || (SecurityContextCheck.checkUserInRole(authentication, EDITOR_ROLE)
        && TagNamespace.GBIF_DEFAULT_TERM.getNamespace().equals(machineTag.getNamespace())
        && userAuthService.allowedToModifyDataset(nameFromContext, targetEntityKey))
      ) {
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
  public void deleteMachineTagsByNamespace(UUID targetEntityKey, String namespace, Authentication authentication) {
    final String nameFromContext = authentication != null ? authentication.getName() : null;

    if (!SecurityContextCheck.checkUserInRole(authentication, UserRoles.ADMIN_ROLE)
      && !userAuthService.allowedToModifyNamespace(nameFromContext, namespace)) {
      throw new WebApplicationException(HttpStatus.FORBIDDEN);
    }
    deleteMachineTags(targetEntityKey, namespace);
  }

  /**
   * It was added because of an ambiguity problem.
   * (Spring can't distinguish {key}/machineTag/{namespace} and {key}/machineTag/{machineTagKey:[0-9]+})
   */
  @DeleteMapping(value = "{key}/machineTag/{parameter}", consumes = MediaType.ALL_VALUE)
  public void deleteMachineTags(@PathVariable("key") UUID targetEntityKey, @PathVariable String parameter,
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
    withMyBatis.deleteMachineTags(mapper, targetEntityKey, namespace, null);
  }

  /**
   * The webservice method to delete all machine tag of a particular name in a namespace.
   * Ensures that the caller is authorized to perform the action by looking at the namespace.
   */
  @DeleteMapping(value = "{key}/machineTag/{namespace}/{name}", consumes = MediaType.ALL_VALUE)
  public void deleteMachineTags(@PathVariable("key") UUID targetEntityKey, @PathVariable String namespace,
                                @PathVariable String name, Authentication authentication) {
    final String nameFromContext = authentication != null ? authentication.getName() : null;

    if (!SecurityContextCheck.checkUserInRole(authentication, UserRoles.ADMIN_ROLE)
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
  public void deleteMachineTags(@NotNull UUID targetEntityKey, @NotNull String namespace, @NotNull String name) {
    withMyBatis.deleteMachineTags(mapper, targetEntityKey, namespace, name);
  }

  @GetMapping(value = "{key}/machineTag")
  @Override
  public List<MachineTag> listMachineTags(@PathVariable("key") UUID targetEntityKey) {
    return withMyBatis.listMachineTags(mapper, targetEntityKey);
  }

  @Override
  public PagingResponse<T> listByMachineTag(String namespace, @Nullable String name, @Nullable String value,
                                            Pageable page) {
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
  @PostMapping(value = "{key}/tag")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public int addTag(@PathVariable("key") UUID targetEntityKey, @RequestBody @NotNull Tag tag,
                    Authentication authentication) {
    tag.setCreatedBy(authentication.getName());
    return addTag(targetEntityKey, tag);
  }

  @Override
  public int addTag(@NotNull UUID targetEntityKey, @NotNull String value) {
    Tag tag = new Tag();
    tag.setValue(value);
    return addTag(targetEntityKey, tag);
  }

  @Override
  public int addTag(UUID targetEntityKey, @Validated({PrePersist.class, Default.class}) Tag tag) {
    int key = withMyBatis.addTag(tagMapper, mapper, targetEntityKey, tag);
    eventManager.post(ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Tag.class));
    return key;
  }

  /**
   * This method ensures that the caller is authorized to perform the action, and then deletes the Tag.
   *
   * @param targetEntityKey key of target entity to delete Tag from
   * @param tagKey          key of Tag to delete
   */
  @DeleteMapping(value = "{key}/tag/{tagKey}", consumes = MediaType.ALL_VALUE)
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public void deleteTag(@PathVariable("key") UUID targetEntityKey, @PathVariable int tagKey) {
    withMyBatis.deleteTag(mapper, targetEntityKey, tagKey);
    eventManager.post(ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Tag.class));
  }

  @GetMapping("{key}/tag")
  @Override
  public List<Tag> listTags(@PathVariable("key") UUID targetEntityKey, @RequestParam(value = "owner", required = false) String owner) {
    return withMyBatis.listTags(mapper, targetEntityKey, owner);
  }

  /**
   * This method ensures that the caller is authorized to perform the action and then adds the server
   * controlled fields for createdBy and modifiedBy.
   *
   * @param targetEntityKey key of target entity to add Contact to
   * @param contact         Contact to add
   * @param authentication  SecurityContext (security related information)
   * @return key of Contact created
   */
  @PostMapping("{key}/contact")
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE, APP_ROLE})
  public int addContact(@PathVariable("key") UUID targetEntityKey, @NotNull @Trim Contact contact,
                        Authentication authentication) {
    contact.setCreatedBy(authentication.getName());
    contact.setModifiedBy(authentication.getName());
    return addContact(targetEntityKey, contact);
  }

  @Override
  public int addContact(UUID targetEntityKey, @Validated({PrePersist.class, Default.class}) Contact contact) {
    int key = withMyBatis.addContact(contactMapper, mapper, targetEntityKey, contact);
    eventManager.post(ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Contact.class));
    return key;
  }

  /**
   * This method ensures that the caller is authorized to perform the action and then adds the server
   * controlled field for modifiedBy.
   *
   * @param targetEntityKey key of target entity to update contact
   * @param contactKey      key of Contact to update
   * @param contact         updated Contact
   */
  @PutMapping("{key}/contact/{contactKey}")
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public void updateContact(@PathVariable("key") UUID targetEntityKey, @PathVariable int contactKey,
                            @RequestBody @NotNull @Trim Contact contact) {
    // for safety, and to match a nice RESTful URL structure
    Preconditions.checkArgument(Integer.valueOf(contactKey).equals(contact.getKey()),
      "Provided contact (key) does not match the path provided");
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    contact.setModifiedBy(authentication.getName());
    updateContact(targetEntityKey, contact);
  }

  @Override
  public void updateContact(UUID targetEntityKey, @Validated({PostPersist.class, Default.class}) Contact contact) {
    withMyBatis.updateContact(contactMapper, mapper, targetEntityKey, contact);
    eventManager.post(ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Contact.class));
  }

  /**
   * This method ensures that the caller is authorized to perform the action.
   *
   * @param targetEntityKey key of target entity to delete Contact from
   * @param contactKey      key of Contact to delete
   */
  @DeleteMapping(value = "{key}/contact/{contactKey}", consumes = MediaType.ALL_VALUE)
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public void deleteContact(@PathVariable("key") UUID targetEntityKey, @PathVariable int contactKey) {
    withMyBatis.deleteContact(mapper, targetEntityKey, contactKey);
    eventManager.post(ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Contact.class));
  }

  @GetMapping("{key}/contact")
  @Override
  public List<Contact> listContacts(@PathVariable("key") UUID targetEntityKey) {
    return withMyBatis.listContacts(mapper, targetEntityKey);
  }

  /**
   * This method ensures that the caller is authorized to perform the action and then adds the server
   * controlled fields for createdBy and modifiedBy.
   *
   * @param targetEntityKey key of target entity to add Endpoint to
   * @param endpoint        Endpoint to add
   * @return key of Endpoint created
   */
  @PostMapping("{key}/endpoint")
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public int addEndpoint(@PathVariable("key") UUID targetEntityKey, @RequestBody @NotNull @Trim Endpoint endpoint,
                         Authentication authentication) {
    endpoint.setCreatedBy(authentication.getName());
    endpoint.setModifiedBy(authentication.getName());
    return addEndpoint(targetEntityKey, endpoint);
  }

  @Override
  public int addEndpoint(UUID targetEntityKey, @Validated({PrePersist.class, Default.class}) Endpoint endpoint) {
    T oldEntity = get(targetEntityKey);
    int key = withMyBatis.addEndpoint(endpointMapper, mapper, targetEntityKey, endpoint, machineTagMapper);
    T newEntity = get(targetEntityKey);
    // posts an UpdateEvent instead of a ChangedComponentEvent, otherwise the crawler would have to start subscribing
    // to ChangedComponentEvent instead just to detect when an endpoint has been added to a Dataset
    eventManager.post(UpdateEvent.newInstance(newEntity, oldEntity, objectClass));
    return key;
  }

  /**
   * This method ensures that the caller is authorized to perform the action, and then deletes the Endpoint.
   *
   * @param targetEntityKey key of target entity to delete Endpoint from
   * @param endpointKey     key of Endpoint to delete
   */
  @DeleteMapping(value = "{key}/endpoint/{endpointKey}", consumes = MediaType.ALL_VALUE)
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public void deleteEndpoint(@PathVariable("key") UUID targetEntityKey, @PathVariable int endpointKey) {
    withMyBatis.deleteEndpoint(mapper, targetEntityKey, endpointKey);
    eventManager.post(ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Endpoint.class));
  }

  @GetMapping("{key}/endpoint")
  @Override
  public List<Endpoint> listEndpoints(@PathVariable("key") UUID targetEntityKey) {
    return withMyBatis.listEndpoints(mapper, targetEntityKey);
  }

  /**
   * This method ensures that the caller is authorized to perform the action and then adds the server
   * controlled field for createdBy.
   *
   * @param targetEntityKey key of target entity to add Identifier to
   * @param identifier      Identifier to add
   * @return key of Identifier created
   */
  @PostMapping("{key}/identifier")
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  public int addIdentifier(@PathVariable("key") UUID targetEntityKey, @RequestBody @NotNull @Trim Identifier identifier,
                           Authentication authentication) {
    identifier.setCreatedBy(authentication.getName());
    return addIdentifier(targetEntityKey, identifier);
  }

  @Override
  public int addIdentifier(UUID targetEntityKey, @Validated({PrePersist.class, Default.class}) Identifier identifier) {
    int key = withMyBatis.addIdentifier(identifierMapper, mapper, targetEntityKey, identifier);
    eventManager.post(ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Identifier.class));
    return key;
  }

  /**
   * This method ensures that the caller is authorized to perform the action, and then deletes the Identifier.
   *
   * @param targetEntityKey key of target entity to delete Identifier from
   * @param identifierKey   key of Identifier to delete
   */
  @DeleteMapping(value = "{key}/identifier/{identifierKey}", consumes = MediaType.ALL_VALUE)
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public void deleteIdentifier(@PathVariable("key") UUID targetEntityKey, @PathVariable("identifierKey") int identifierKey) {
    withMyBatis.deleteIdentifier(mapper, targetEntityKey, identifierKey);
    eventManager.post(ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Identifier.class));
  }

  @GetMapping("{key}/identifier")
  @Override
  public List<Identifier> listIdentifiers(@PathVariable("key") UUID targetEntityKey) {
    return withMyBatis.listIdentifiers(mapper, targetEntityKey);
  }

  /**
   * Override this method to extract the entity key that governs security rights for creating.
   * If null is returned only admins are allowed to create new entities which is the default.
   */
  protected List<UUID> owningEntityKeys(@NotNull T entity) {
    return new ArrayList<>();
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
