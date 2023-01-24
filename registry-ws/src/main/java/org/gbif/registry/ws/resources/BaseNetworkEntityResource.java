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
import org.gbif.registry.persistence.mapper.BaseNetworkEntityMapper;
import org.gbif.registry.persistence.mapper.CommentMapper;
import org.gbif.registry.persistence.mapper.ContactMapper;
import org.gbif.registry.persistence.mapper.EndpointMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.service.MapperServiceLocator;
import org.gbif.registry.service.WithMyBatis;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.Valid;
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
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
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

  /**
   * Default key parameter for entity requests.
   */
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Parameter(
    name = "key",
    description = "The key of the entity (dataset, organization, network etc.)",
    in = ParameterIn.PATH)
  @interface DefaultEntityKeyParameter {}

  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Parameter(
    name = "q",
    description =
      "Simple full text search parameter. The value for this parameter can be a simple word or a phrase. Wildcards are not supported",
    schema = @Schema(implementation = String.class),
    in = ParameterIn.QUERY)
  @interface DefaultQParameter {}

  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Parameter(
    name = "hl",
    description = "Set `hl=true` to highlight terms matching the query when in fulltext search fields. The highlight " +
      "will be an emphasis tag of class `gbifH1`.",
    schema = @Schema(implementation = Boolean.class),
    in = ParameterIn.QUERY)
  @interface DefaultHlParameter {}

  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Parameters(
    value = {
      @Parameter(
        name = "identifierType",
        description = "An identifier type for the identifier parameter.",
        schema = @Schema(implementation = String.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "identifier",
        description = "An identifier of the type given by the identifierType parameter, for example a DOI or UUID.",
        schema = @Schema(implementation = String.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "machineTagNamespace",
        description = "Filters for entities with a machine tag in the specified namespace.",
        schema = @Schema(implementation = String.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "machineTagName",
        description = "Filters for entities with a machine tag with the specified name (use in combination with the machineTagNamespace parameter).",
        schema = @Schema(implementation = String.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "machineTagValue",
        description = "Filters for entities with a machine tag with the specified value (use in combination with the machineTagNamespace and machineTagName parameters).",
        schema = @Schema(implementation = String.class),
        in = ParameterIn.QUERY),

      @Parameter(
        name = "request",
        hidden = true
      )
    })
  @DefaultQParameter
  @DefaultOffsetLimitParameters
  @interface DefaultSimpleSearchParameters {}

  /**
   * The usual limit and offset parameters
   */
  @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @Parameters(
    value = {
      @Parameter(
        name = "limit",
        description = "Controls the number of results in the page. Using too high a value will be overwritten with the " +
          "default maximum threshold, depending on the service. Sensible defaults are used so this may be omitted.",
        schema = @Schema(implementation = Integer.class, minimum = "0"),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "offset",
        description = "Determines the offset for the search results. A limit of 20 and offset of 40 will get the third " +
          "page of 20 results. Some services have a maximum offset.",
        schema = @Schema(implementation = Integer.class, minimum = "0"),
        in = ParameterIn.QUERY),
    }
  )
  @interface DefaultOffsetLimitParameters {}

  /**
   * The usual (search) facet parameters
   */
  @Target({PARAMETER, METHOD, FIELD, ANNOTATION_TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Inherited
  @Parameters(
    value = {
      @Parameter(
        name = "facet",
        description =
          "A facet name used to retrieve the most frequent values for a field. Facets are allowed for all the parameters except for: eventDate, geometry, lastInterpreted, locality, organismId, stateProvince, waterBody. This parameter may by repeated to request multiple facets, as in this example /occurrence/search?facet=datasetKey&facet=basisOfRecord&limit=0",
        schema = @Schema(implementation = String.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "facetMincount",
        description =
          "Used in combination with the facet parameter. Set facetMincount={#} to exclude facets with a count less than {#}, e.g. /search?facet=type&limit=0&facetMincount=10000 only shows the type value 'OCCURRENCE' because 'CHECKLIST' and 'METADATA' have counts less than 10000.",
        schema = @Schema(implementation = Integer.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "facetMultiselect",
        description =
          "Used in combination with the facet parameter. Set facetMultiselect=true to still return counts for values that are not currently filtered, e.g. /search?facet=type&limit=0&type=CHECKLIST&facetMultiselect=true still shows type values 'OCCURRENCE' and 'METADATA' even though type is being filtered by type=CHECKLIST",
        schema = @Schema(implementation = Boolean.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "facetLimit",
        description =
          "Facet parameters allow paging requests using the parameters facetOffset and facetLimit",
        schema = @Schema(implementation = Integer.class),
        in = ParameterIn.QUERY),
      @Parameter(
        name = "facetOffset",
        description =
          "Facet parameters allow paging requests using the parameters facetOffset and facetLimit",
        schema = @Schema(implementation = Integer.class, minimum = "0"),
        in = ParameterIn.QUERY)
    }
  )
  @interface DefaultFacetParameters {}

  /**
   * Documents responses to every read-only operation on subentities: comments, tags, machine tags, etc.
   */
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @ApiResponses({
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
    @ApiResponse(responseCode = "404", description = "Entity or subentity not Found", content = @Content),
    @ApiResponse(responseCode = "500", description = "System failure – try again", content = @Content)})
  @interface DefaultUnsuccessfulReadResponses {}

  /**
   * Documents responses to every write operation on subentities: comments, tags, machine tags, etc.
   */
  @Target({ElementType.METHOD, ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @ApiResponses({
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)})
  @interface DefaultUnsuccessfulWriteResponses {}

  // We use post rather than get because we expect large numbers of keys to be sent
  @Hidden // TODO: Not sure if this is supposed to be public API.
  @PostMapping(value = "titles", consumes = MediaType.APPLICATION_JSON_VALUE)
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
   * This method ensures that the path variable for the key matches the entity's key, ensures that
   * the caller is authorized to perform the action and then adds the server controlled field
   * modifiedBy.
   *
   * @param entity entity that extends NetworkEntity
   */
  @Operation(
    operationId = "update",
    summary = "Updates the existing record",
    description = "Updates the existing record.  Note contacts, endpoints, identifiers, tags, machine tags, comments and " +
      "metadata descriptions are not changed with this method.")
  @DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "204",
    description = "Record updated")
  @DefaultUnsuccessfulReadResponses
  @DefaultUnsuccessfulWriteResponses
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
    summary = "Adds a comment to the record")
  @DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "200",
    description = "Comment added, comment key returned")
  @DefaultUnsuccessfulReadResponses
  @DefaultUnsuccessfulWriteResponses
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
    summary = "Deletes a comment from the record")
  @DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "204",
    description = "Comment deleted")
  @DefaultUnsuccessfulReadResponses
  @DefaultUnsuccessfulWriteResponses
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
    summary = "Retrieves all comments of the record")
  @DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "200",
    description = "List of comments")
  @DefaultUnsuccessfulReadResponses
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
    summary = "Adds a machine tag to the record")
  @DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "204",
    description = "Machine tag added, machine tag key returned")
  @DefaultUnsuccessfulReadResponses
  @DefaultUnsuccessfulWriteResponses
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
    summary = "Deletes a machine tag from the record")
  @DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "204",
    description = "Machine tag deleted")
  @DefaultUnsuccessfulReadResponses
  @DefaultUnsuccessfulWriteResponses
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
    summary = "Deletes all machine tags in a namespace from the record")
  @DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "204",
    description = "Machine tags in namespace deleted")
  @DefaultUnsuccessfulReadResponses
  @DefaultUnsuccessfulWriteResponses
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
    summary = "Deletes all machine tags of a name in a namespace from the record")
  @DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "204",
    description = "Named machine tags in namespace deleted")
  @DefaultUnsuccessfulReadResponses
  @DefaultUnsuccessfulWriteResponses
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
    summary = "Lists all machine tags on the record")
  @DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "200",
    description = "Machine tags list")
  @DefaultUnsuccessfulReadResponses
  @DefaultUnsuccessfulWriteResponses
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
  @Operation(
    operationId = "addTag",
    summary = "Adds a tag to the record")
  @DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "200",
    description = "Tag added, tag key returned")
  @DefaultUnsuccessfulReadResponses
  @DefaultUnsuccessfulWriteResponses
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
    summary = "Deletes a tag from the record")
  @DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "204",
    description = "Tag deleted")
  @DefaultUnsuccessfulReadResponses
  @DefaultUnsuccessfulWriteResponses
  @DeleteMapping("{key}/tag/{tagKey}")
  @Secured({ADMIN_ROLE, EDITOR_ROLE})
  @Override
  public void deleteTag(@PathVariable("key") UUID targetEntityKey, @PathVariable int tagKey) {
    mapper.deleteTag(targetEntityKey, tagKey);
    eventManager.post(ChangedComponentEvent.newInstance(targetEntityKey, objectClass, Tag.class));
  }

  @Operation(
    operationId = "getTag",
    summary = "Retrieves all tags of the record")
  @DefaultEntityKeyParameter
  @Parameter(name = "owner", hidden = true)
  @ApiResponse(
    responseCode = "200",
    description = "Tag list",
    content = @Content(
      schema = @Schema(implementation = Tag.class),
      examples = @ExampleObject(value = "[\n" +
        "  {\n" +
        "    \"key\": 1234,\n" +
        "    \"name\": \"some name\",\n" +
        "    \"value\": \"some value\",\n" +
        "    \"createdBy\": \"MattBlissett\",\n" +
        "    \"created\": \"2023-01-24T11:45:06.310Z\"\n" +
        "  }\n" +
        "]")))
  @DefaultUnsuccessfulReadResponses
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
    summary = "Adds a contact to the record")
  @DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "200",
    description = "Contact added, contact key returned")
  @DefaultUnsuccessfulReadResponses
  @DefaultUnsuccessfulWriteResponses
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
    summary = "Updates an existing contact on the record")
  @DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "204",
    description = "Contact updated")
  @DefaultUnsuccessfulReadResponses
  @DefaultUnsuccessfulWriteResponses
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
    summary = "Deletes a contact from the record")
  @DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "204",
    description = "Contact deleted")
  @DefaultUnsuccessfulReadResponses
  @DefaultUnsuccessfulWriteResponses
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
    summary = "Retrieves all contacts of the record")
  @DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "200",
    description = "List of contacts")
  @DefaultUnsuccessfulReadResponses
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
    summary = "Adds an endpoint to the record")
  @DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "200",
    description = "Endpoint added, endpoint key returned")
  @DefaultUnsuccessfulReadResponses
  @DefaultUnsuccessfulWriteResponses
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
    summary = "Deletes an endpoint from the record")
  @DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "204",
    description = "Endpoint deleted")
  @DefaultUnsuccessfulReadResponses
  @DefaultUnsuccessfulWriteResponses
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
    summary = "Retrieves all endpoints of the record")
  @DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "200",
    description = "List of endpoints")
  @DefaultUnsuccessfulReadResponses
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
    summary = "Adds an identifier to the record")
  @DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "200",
    description = "Identifier added, identifier key returned")
  @DefaultUnsuccessfulReadResponses
  @DefaultUnsuccessfulWriteResponses
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
    summary = "Deletes an identifier from the record")
  @DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "204",
    description = "Endpoint deleted")
  @DefaultUnsuccessfulReadResponses
  @DefaultUnsuccessfulWriteResponses
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
    summary = "Retrieves all identifiers of the record")
  @DefaultEntityKeyParameter
  @ApiResponse(
    responseCode = "200",
    description = "Identifiers list")
  @DefaultUnsuccessfulReadResponses
  @DefaultUnsuccessfulWriteResponses
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
}
