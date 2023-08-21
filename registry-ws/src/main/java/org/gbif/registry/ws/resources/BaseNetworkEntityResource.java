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
package org.gbif.registry.ws.resources;

import org.gbif.api.annotation.Trim;
import org.gbif.api.documentation.CommonParameters;
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
import org.gbif.api.util.Range;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.TagName;
import org.gbif.api.vocabulary.TagNamespace;
import org.gbif.registry.events.ChangedComponentEvent;
import org.gbif.registry.events.CreateEvent;
import org.gbif.registry.events.DeleteEvent;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.UpdateEvent;
import org.gbif.registry.persistence.mapper.BaseNetworkEntityMapper;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.ContactMapper;
import org.gbif.registry.persistence.mapper.EndpointMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.params.BaseListParams;
import org.gbif.registry.persistence.service.MapperServiceLocator;
import org.gbif.registry.service.WithMyBatis;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.APP_ROLE;
import static org.gbif.registry.security.UserRoles.EDITOR_ROLE;
import static org.gbif.registry.security.UserRoles.IPT_ROLE;

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
@SuppressWarnings("unchecked")
@Validated
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE) // TODO: remove P
public abstract class BaseNetworkEntityResource<T extends NetworkEntity, P extends BaseListParams>
    implements NetworkEntityService<T> {

  private static final Logger LOG = LoggerFactory.getLogger(BaseNetworkEntityResource.class);

  private final BaseNetworkEntityMapper<T> mapper;
  private final CommentMapper commentMapper;
  private final MachineTagMapper machineTagMapper;
  private final TagMapper tagMapper;
  private final ContactMapper contactMapper;
  private final EndpointMapper endpointMapper;
  private final IdentifierMapper identifierMapper;
  private final EventManager eventManager;
  private final WithMyBatis withMyBatis;
  private final Class<T> objectClass;

  protected BaseNetworkEntityResource(
      BaseNetworkEntityMapper<T> mapper,
      MapperServiceLocator mapperServiceLocator,
      Class<T> objectClass,
      EventManager eventManager,
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
    this.withMyBatis = withMyBatis;
  }

  /** Documentation for a SearchRequest. */
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Parameters(
      value = {
        @Parameter(
            name = "identifierType",
            description = "An identifier type for the identifier parameter.",
            schema = @Schema(implementation = IdentifierType.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "identifier",
            description =
                "An identifier of the type given by the identifierType parameter, for example a DOI or UUID.",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "machineTagNamespace",
            description = "Filters for entities with a machine tag in the specified namespace.",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "machineTagName",
            description =
                "Filters for entities with a machine tag with the specified name (use in combination with the machineTagNamespace parameter).",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "machineTagValue",
            description =
                "Filters for entities with a machine tag with the specified value (use in combination with the machineTagNamespace and machineTagName parameters).",
            schema = @Schema(implementation = String.class),
            in = ParameterIn.QUERY),
        @Parameter(
            name = "modified",
            description =
                "The modified date of the dataset. Accepts ranges and a '*' can be used as a wildcard, e.g.:modified=2023-04-01,*",
            schema = @Schema(implementation = Range.class),
            in = ParameterIn.QUERY,
            explode = Explode.TRUE),
        @Parameter(name = "request", hidden = true)
      })
  @CommonParameters.QParameter
  @Pageable.OffsetLimitParameters
  @interface SimpleSearchParameters {}

  /**
   * This method ensures that the caller is authorized to perform the action and then adds the
   * server controlled fields for createdBy and modifiedBy. It then creates the entity.
   *
   * @param entity entity that extends NetworkEntity
   * @return key of entity created
   */
  // OpenAPI documentation is on the subclasses.
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE, IPT_ROLE})
  @Override
  public UUID create(@RequestBody @Trim T entity) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String nameFromContext = authentication != null ? authentication.getName() : null;
    entity.setCreatedBy(nameFromContext);
    entity.setModifiedBy(nameFromContext);

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
  // OpenAPI documentation is on the subclasses.
  @DeleteMapping("{key}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE, IPT_ROLE})
  @Transactional
  @Override
  public void delete(@PathVariable UUID key) {
    // the following lines allow to set the "modifiedBy" to the user who actually deletes the
    // entity.
    // the api delete(UUID) should be changed eventually
    T objectToDelete = get(key);
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (objectToDelete != null) {
      objectToDelete.setModifiedBy(authentication.getName());
      withMyBatis.update(mapper, objectToDelete);
      mapper.delete(key);
      eventManager.post(DeleteEvent.newInstance(objectToDelete, objectClass));
    }
  }

  @Nullable
  @Override
  public T get(UUID key) {
    return mapper.get(key);
  }

  // We use post rather than get because we expect large numbers of keys to be sent
  @Hidden // TODO: Not sure if this is supposed to be public API.
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
  @Deprecated
  public PagingResponse<T> list(Pageable page) {
    return list(BaseListParams.builder().page(page).build());
  }

  @Override
  @Deprecated
  public PagingResponse<T> search(String query, Pageable page) {
    String q = query != null ? Strings.emptyToNull(CharMatcher.WHITESPACE.trimFrom(query)) : query;
    return list(BaseListParams.builder().query(q).page(page).build());
  }

  @Override
  @Deprecated
  public PagingResponse<T> listByIdentifier(IdentifierType type, String identifier, Pageable page) {
    return list(
        BaseListParams.builder().identifierType(type).identifier(identifier).page(page).build());
  }

  @Override
  @Deprecated
  public PagingResponse<T> listByIdentifier(String identifier, Pageable page) {
    return list(BaseListParams.builder().identifier(identifier).page(page).build());
  }

  /**
   * This method ensures that the path variable for the key matches the entity's key, ensures that
   * the caller is authorized to perform the action and then adds the server controlled field
   * modifiedBy.
   *
   * @param entity entity that extends NetworkEntity
   */
  // OpenAPI documentation on subclass methods.
  @PutMapping(value = "{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Secured({ADMIN_ROLE, EDITOR_ROLE, IPT_ROLE})
  @Validated({PostPersist.class, Default.class})
  @Trim
  @Transactional
  public void update(@PathVariable("key") UUID key, @Valid @RequestBody @Trim T entity) {
    checkArgument(key.equals(entity.getKey()));
    update(entity);
  }

  @Transactional
  @Override
  public void update(T entity) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null) {
      entity.setModifiedBy(authentication.getName());
    }

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
  @Operation(
      operationId = "addComment",
      summary = "Add a comment to the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0401")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Comment added, comment key returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(value = "{key}/comment", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE, APP_ROLE})
  @Override
  public int addComment(
      @PathVariable("key") UUID targetEntityKey, @RequestBody @Trim Comment comment) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    comment.setCreatedBy(authentication.getName());
    comment.setModifiedBy(authentication.getName());
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
  @Operation(
      operationId = "deleteComment",
      summary = "Delete a comment from the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0402")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Comment deleted")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}/comment/{commentKey}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public void deleteComment(
      @PathVariable("key") UUID targetEntityKey, @PathVariable int commentKey) {
    mapper.deleteComment(targetEntityKey, commentKey);
    eventManager.post(
        ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Comment.class));
  }

  @Operation(
      operationId = "getComment",
      summary = "Retrieve all comments of the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0400")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "List of comments")
  @Docs.DefaultUnsuccessfulReadResponses
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
  @Operation(
      operationId = "addMachineTag",
      summary = "Add a machine tag to the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0441")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Machine tag added, machine tag key returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(value = "{key}/machineTag", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Trim
  @Transactional
  @Override
  public int addMachineTag(
      @PathVariable("key") UUID targetEntityKey, @RequestBody @Trim MachineTag machineTag) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String nameFromContext = authentication != null ? authentication.getName() : null;

    machineTag.setCreatedBy(nameFromContext);
    return withMyBatis.addMachineTag(machineTagMapper, mapper, targetEntityKey, machineTag);
  }

  @Transactional
  @Override
  public int addMachineTag(UUID targetEntityKey, String namespace, String name, String value) {
    MachineTag machineTag = new MachineTag();
    machineTag.setNamespace(namespace);
    machineTag.setName(name);
    machineTag.setValue(value);
    return addMachineTag(targetEntityKey, machineTag);
  }

  @Transactional
  @Override
  public int addMachineTag(UUID targetEntityKey, TagName tagName, String value) {
    MachineTag machineTag = MachineTag.newInstance(tagName, value);
    return addMachineTag(targetEntityKey, machineTag);
  }

  /**
   * The webservice method to delete a machine tag. Ensures that the caller is authorized to perform
   * the action by looking at the namespace.
   */
  @Operation(
      operationId = "deleteMachineTag",
      summary = "Delete a machine tag from the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0442")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Machine tag deleted")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}/machineTag/{machineTagKey:[0-9]+}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public void deleteMachineTag(
      @PathVariable("key") UUID targetEntityKey, @PathVariable("machineTagKey") int machineTagKey) {
    mapper.deleteMachineTag(targetEntityKey, machineTagKey);
  }

  /**
   * The webservice method to delete all machine tag in a namespace. Ensures that the caller is
   * authorized to perform the action by looking at the namespace.
   */
  @Operation(
      operationId = "deleteMachineTagsInNamespace",
      summary = "Delete all machine tags in a namespace from the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0443")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Machine tags in namespace deleted")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}/machineTag/{namespace:.*[^0-9]+.*}")
  @Secured(ADMIN_ROLE)
  @Override
  public void deleteMachineTags(
      @PathVariable("key") UUID targetEntityKey, @PathVariable("namespace") String namespace) {
    mapper.deleteMachineTags(targetEntityKey, namespace, null);
  }

  @Override
  public void deleteMachineTags(UUID targetEntityKey, TagNamespace tagNamespace) {
    deleteMachineTags(targetEntityKey, tagNamespace.getNamespace());
  }

  /**
   * The webservice method to delete all machine tag of a particular name in a namespace. Ensures
   * that the caller is authorized to perform the action by looking at the namespace.
   */
  @Operation(
      operationId = "deleteMachineTagInNamespaceName",
      summary = "Delete all machine tags of a name in a namespace from the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0444")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Named machine tags in namespace deleted")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}/machineTag/{namespace}/{name}")
  @Secured(ADMIN_ROLE)
  @Override
  public void deleteMachineTags(
      @PathVariable("key") UUID targetEntityKey,
      @PathVariable String namespace,
      @PathVariable String name) {
    mapper.deleteMachineTags(targetEntityKey, namespace, name);
  }

  @Override
  public void deleteMachineTags(UUID targetEntityKey, TagName tagName) {
    deleteMachineTags(targetEntityKey, tagName.getNamespace().getNamespace(), tagName.getName());
  }

  @Operation(
      operationId = "listMachineTag",
      summary = "List all machine tags on the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0440")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Machine tags list")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @SuppressWarnings("unchecked")
  @GetMapping("{key}/machineTag")
  @Override
  public List<MachineTag> listMachineTags(@PathVariable("key") UUID targetEntityKey) {
    return mapper.listMachineTags(targetEntityKey);
  }

  @Override
  @Deprecated
  public PagingResponse<T> listByMachineTag(
      String namespace, String name, String value, Pageable page) {
    return list(
        BaseListParams.builder()
            .mtNamespace(namespace)
            .mtName(name)
            .mtValue(value)
            .page(page)
            .build());
  }

  /**
   * This method ensures that the caller is authorized to perform the action and then adds the
   * server controlled fields for createdBy.
   *
   * @param targetEntityKey key of target entity to add Tag to
   * @param tag Tag to add
   * @return key of Tag created
   */
  @Operation(
      operationId = "addTag",
      summary = "Add a tag to the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0451")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Tag added, tag key returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(value = "{key}/tag", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public int addTag(@PathVariable("key") UUID targetEntityKey, @RequestBody Tag tag) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    tag.setCreatedBy(authentication.getName());
    int key = withMyBatis.addTag(tagMapper, mapper, targetEntityKey, tag);
    eventManager.post(ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Tag.class));
    return key;
  }

  @Validated({PrePersist.class, Default.class})
  @Override
  public int addTag(UUID targetEntityKey, String value) {
    Tag tag = new Tag();
    tag.setValue(value);
    return addTag(targetEntityKey, tag);
  }

  /**
   * This method ensures that the caller is authorized to perform the action, and then deletes the
   * Tag.
   *
   * @param targetEntityKey key of target entity to delete Tag from
   * @param tagKey key of Tag to delete
   */
  @Operation(
      operationId = "deleteTag",
      summary = "Delete a tag from the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0452")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Tag deleted")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}/tag/{tagKey}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public void deleteTag(@PathVariable("key") UUID targetEntityKey, @PathVariable int tagKey) {
    mapper.deleteTag(targetEntityKey, tagKey);
    eventManager.post(ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Tag.class));
  }

  @Operation(
      operationId = "getTag",
      summary = "Retrieve all tags of the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0450")))
  @Docs.DefaultEntityKeyParameter
  @Parameter(name = "owner", hidden = true)
  @ApiResponse(
      responseCode = "200",
      description = "Tag list",
      content =
          @Content(
              schema = @Schema(implementation = Tag.class),
              examples =
                  @ExampleObject(
                      value =
                          "[\n"
                              + "  {\n"
                              + "    \"key\": 1234,\n"
                              + "    \"name\": \"some name\",\n"
                              + "    \"value\": \"some value\",\n"
                              + "    \"createdBy\": \"MattBlissett\",\n"
                              + "    \"created\": \"2023-01-24T11:45:06.310Z\"\n"
                              + "  }\n"
                              + "]")))
  @Docs.DefaultUnsuccessfulReadResponses
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
   * @return key of Contact created
   */
  @Operation(
      operationId = "addContact",
      summary = "Add a contact to the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0412")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Contact added, contact key returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(value = "{key}/contact", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE, APP_ROLE, IPT_ROLE})
  @Override
  public int addContact(
      @PathVariable("key") UUID targetEntityKey, @RequestBody @Trim Contact contact) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    contact.setCreatedBy(authentication.getName());
    contact.setModifiedBy(authentication.getName());
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
   * @param contact updated Contact
   */
  @Operation(
      operationId = "updateContact",
      summary = "Update an existing contact on the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0413")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Contact updated")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PutMapping(
      value = {"{key}/contact", "{key}/contact/{contactKey}"},
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PostPersist.class, Default.class})
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE, IPT_ROLE})
  @Override
  public void updateContact(
      @PathVariable("key") UUID targetEntityKey, @RequestBody @Trim Contact contact) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    contact.setModifiedBy(authentication.getName());
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
  @Operation(
      operationId = "deleteContact",
      summary = "Delete a contact from the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0414")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Contact deleted")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}/contact/{contactKey}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public void deleteContact(
      @PathVariable("key") UUID targetEntityKey, @PathVariable int contactKey) {
    mapper.deleteContact(targetEntityKey, contactKey);
    eventManager.post(
        ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Contact.class));
  }

  @Operation(
      operationId = "getContact",
      summary = "Retrieve all contacts of the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0411")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "List of contacts")
  @Docs.DefaultUnsuccessfulReadResponses
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
  @Operation(
      operationId = "addEndpoint",
      summary = "Add an endpoint to the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0421")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Endpoint added, endpoint key returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(value = "{key}/endpoint", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE, IPT_ROLE})
  @Override
  public int addEndpoint(
      @PathVariable("key") UUID targetEntityKey, @RequestBody @Trim Endpoint endpoint) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    endpoint.setCreatedBy(authentication.getName());
    endpoint.setModifiedBy(authentication.getName());
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
  @Operation(
      operationId = "deleteEndpoint",
      summary = "Delete an endpoint from the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0422")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Endpoint deleted")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}/endpoint/{endpointKey}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE, IPT_ROLE})
  @Override
  public void deleteEndpoint(
      @PathVariable("key") UUID targetEntityKey, @PathVariable int endpointKey) {
    withMyBatis.deleteEndpoint(mapper, targetEntityKey, endpointKey);
    eventManager.post(
        ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Endpoint.class));
  }

  @Operation(
      operationId = "getEndpoint",
      summary = "Retrieve all endpoints of the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0420")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "List of endpoints")
  @Docs.DefaultUnsuccessfulReadResponses
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
  @Operation(
      operationId = "addIdentifier",
      summary = "Add an identifier to the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0431")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Identifier added, identifier key returned")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @PostMapping(value = "{key}/identifier", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Validated({PrePersist.class, Default.class})
  @Trim
  @Transactional
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public int addIdentifier(
      @PathVariable("key") UUID targetEntityKey, @RequestBody @Trim Identifier identifier) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    identifier.setCreatedBy(authentication.getName());
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
  @Operation(
      operationId = "deleteIdentifier",
      summary = "Delete an identifier from the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0432")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "204", description = "Endpoint deleted")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}/identifier/{identifierKey}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public void deleteIdentifier(
      @PathVariable("key") UUID targetEntityKey, @PathVariable("identifierKey") int identifierKey) {
    mapper.deleteIdentifier(targetEntityKey, identifierKey);
    eventManager.post(
        ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Identifier.class));
  }

  @Operation(
      operationId = "getIdentifier",
      summary = "Retrieve all identifiers of the record",
      extensions =
          @Extension(
              name = "Order",
              properties = @ExtensionProperty(name = "Order", value = "0430")))
  @Docs.DefaultEntityKeyParameter
  @ApiResponse(responseCode = "200", description = "Identifiers list")
  @Docs.DefaultUnsuccessfulReadResponses
  @Docs.DefaultUnsuccessfulWriteResponses
  @GetMapping("{key}/identifier")
  @Override
  public List<Identifier> listIdentifiers(@PathVariable("key") UUID targetEntityKey) {
    return mapper.listIdentifiers(targetEntityKey);
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

  protected String parseQuery(String q) {
    return q != null ? Strings.emptyToNull(CharMatcher.WHITESPACE.trimFrom(q)) : q;
  }

  protected Date parseFrom(Range<LocalDate> range) {
    return range != null && range.lowerEndpoint() != null
        ? Date.from(range.lowerEndpoint().atStartOfDay(ZoneId.systemDefault()).toInstant())
        : null;
  }

  protected Date parseTo(Range<LocalDate> range) {
    return range != null && range.upperEndpoint() != null
        ? Date.from(range.upperEndpoint().atStartOfDay(ZoneId.systemDefault()).toInstant())
        : null;
  }

  protected abstract PagingResponse<T> list(BaseListParams params);
}
