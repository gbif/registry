package org.gbif.registry.ws.resources.collections;

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.*;
import org.gbif.api.service.collections.CrudService;
import org.gbif.api.service.registry.IdentifierService;
import org.gbif.api.service.registry.MachineTagService;
import org.gbif.api.service.registry.TagService;
import org.gbif.api.vocabulary.TagName;
import org.gbif.api.vocabulary.TagNamespace;
import org.gbif.registry.events.ChangedComponentEvent;
import org.gbif.registry.events.collections.DeleteCollectionEntityEvent;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.BaseMapper;
import org.gbif.registry.ws.guice.Trim;
import org.gbif.registry.ws.security.EditorAuthorizationService;
import org.gbif.ws.server.interceptor.NullToNotFound;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.google.common.eventbus.EventBus;
import org.apache.bval.guice.Validate;
import org.mybatis.guice.transactional.Transactional;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.GRSCICOLL_ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.GRSCICOLL_EDITOR_ROLE;

import static com.google.common.base.Preconditions.checkArgument;

/** Base class to implement the CRUD methods of a {@link CollectionEntity}. */
public abstract class BaseCollectionEntityResource<
        T extends CollectionEntity & Taggable & Identifiable & MachineTaggable>
    implements CrudService<T>, TagService, IdentifierService, MachineTagService {

  private final BaseMapper<T> baseMapper;
  private final Class<T> objectClass;
  private final TagMapper tagMapper;
  private final MachineTagMapper machineTagMapper;
  private final IdentifierMapper identifierMapper;
  private final EventBus eventBus;
  final EditorAuthorizationService userAuthService;

  protected BaseCollectionEntityResource(
      BaseMapper<T> baseMapper,
      TagMapper tagMapper,
      MachineTagMapper machineTagMapper,
      IdentifierMapper identifierMapper,
      EditorAuthorizationService userAuthService,
      EventBus eventBus,
      Class<T> objectClass) {
    this.baseMapper = baseMapper;
    this.tagMapper = tagMapper;
    this.machineTagMapper = machineTagMapper;
    this.identifierMapper = identifierMapper;
    this.eventBus = eventBus;
    this.objectClass = objectClass;
    this.userAuthService = userAuthService;
  }

  @POST
  @Trim
  @Validate
  @Transactional
  @RolesAllowed({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  public UUID create(@NotNull T entity, @Context SecurityContext security) {
    entity.setCreatedBy(security.getUserPrincipal().getName());
    entity.setModifiedBy(security.getUserPrincipal().getName());
    return create(entity);
  }

  @DELETE
  @Path("{key}")
  @Validate
  @Transactional
  @RolesAllowed({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  public void delete(@PathParam("key") @NotNull UUID key, @Context SecurityContext security) {
    T entityToDelete = get(key);
    entityToDelete.setModifiedBy(security.getUserPrincipal().getName());
    update(entityToDelete);

    delete(key);
  }

  @Transactional
  @Validate
  @Override
  public void delete(@NotNull UUID key) {
    T objectToDelete = get(key);
    baseMapper.delete(key);
    eventBus.post(DeleteCollectionEntityEvent.newInstance(objectToDelete, objectClass));
  }

  @GET
  @Path("{key}")
  @Nullable
  @NullToNotFound
  @Validate(validateReturnedValue = true)
  @Override
  public T get(@PathParam("key") @NotNull UUID key) {
    return baseMapper.get(key);
  }

  @PUT
  @Path("{key}")
  @Validate
  @Transactional
  @RolesAllowed({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  public void update(
      @PathParam("key") @NotNull UUID key,
      @NotNull @Trim T entity,
      @Context SecurityContext security) {
    checkArgument(
        key.equals(entity.getKey()), "Provided entity must have the same key as the resource URL");
    entity.setModifiedBy(security.getUserPrincipal().getName());
    update(entity);
  }

  @POST
  @Path("{key}/identifier")
  @Trim
  @RolesAllowed({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  public int addIdentifier(
    @PathParam("key") @NotNull UUID entityKey, @NotNull Identifier identifier, @Context SecurityContext security
  ) {
    identifier.setCreatedBy(security.getUserPrincipal().getName());
    return addIdentifier(entityKey, identifier);
  }

  @Validate(groups = {PrePersist.class, Default.class})
  @Override
  public int addIdentifier(@NotNull UUID entityKey, @Valid @NotNull Identifier identifier) {
    int identifierKey = WithMyBatis.addIdentifier(identifierMapper, baseMapper, entityKey, identifier);
    eventBus.post(ChangedComponentEvent.newInstance(entityKey, objectClass, Identifier.class));
    return identifierKey;
  }

  @DELETE
  @Path("{key}/identifier/{identifierKey}")
  @RolesAllowed({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  @Transactional
  @Override
  public void deleteIdentifier(
    @PathParam("key") @NotNull UUID entityKey,
    @PathParam("identifierKey") int identifierKey
  ) {
    WithMyBatis.deleteIdentifier(baseMapper, entityKey, identifierKey);
    eventBus.post(ChangedComponentEvent.newInstance(entityKey, objectClass, Identifier.class));
  }

  @GET
  @Path("{key}/identifier")
  @Nullable
  @Validate(validateReturnedValue = true)
  @Override
  public List<Identifier> listIdentifiers(@PathParam("key") @NotNull UUID key) {
    return WithMyBatis.listIdentifiers(baseMapper, key);
  }

  @POST
  @Path("{key}/tag")
  @Trim
  @RolesAllowed({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  public int addTag(@PathParam("key") @NotNull UUID entityKey, @NotNull Tag tag, @Context SecurityContext security) {
    tag.setCreatedBy(security.getUserPrincipal().getName());
    return addTag(entityKey, tag);
  }

  @Override
  public int addTag(@NotNull UUID key, @NotNull String value) {
    Tag tag = new Tag();
    tag.setValue(value);
    return addTag(key, tag);
  }

  @Validate(groups = {PrePersist.class, Default.class})
  @Override
  public int addTag(@NotNull UUID entityKey, @Valid @NotNull Tag tag) {
    int tagKey = WithMyBatis.addTag(tagMapper, baseMapper, entityKey, tag);
    eventBus.post(ChangedComponentEvent.newInstance(entityKey, objectClass, Tag.class));
    return tagKey;
  }

  @DELETE
  @Path("{key}/tag/{tagKey}")
  @RolesAllowed({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  @Transactional
  @Override
  public void deleteTag(@PathParam("key") @NotNull UUID entityKey, @PathParam("tagKey") int tagKey) {
    WithMyBatis.deleteTag(baseMapper, entityKey, tagKey);
    eventBus.post(ChangedComponentEvent.newInstance(entityKey, objectClass, Tag.class));
  }

  @GET
  @Path("{key}/tag")
  @Nullable
  @Validate(validateReturnedValue = true)
  @Override
  public List<Tag> listTags(@PathParam("key") @NotNull UUID key, @QueryParam("owner") @Nullable String owner) {
    return WithMyBatis.listTags(baseMapper, key, owner);
  }

  /**
   * Adding most machineTags is restricted based on the namespace.
   *
   * For some tags, it is restricted based on the editing role as usual.
   *
   * @param targetEntityKey key of target entity to add MachineTag to
   * @param machineTag MachineTag to add
   * @param security SecurityContext (security related information)
   * @return key of MachineTag created
   */
  @POST
  @Path("{key}/machineTag")
  @Trim
  @Transactional
  public int addMachineTag(@PathParam("key") UUID targetEntityKey, @NotNull @Trim MachineTag machineTag,
                           @Context SecurityContext security) {

    if (security.isUserInRole(GRSCICOLL_ADMIN_ROLE)
        || userAuthService.allowedToModifyNamespace(security.getUserPrincipal(), machineTag.getNamespace())
        || (security.isUserInRole(GRSCICOLL_EDITOR_ROLE)
            && TagNamespace.GBIF_DEFAULT_TERM.getNamespace().equals(machineTag.getNamespace())
            && userAuthService.allowedToModifyDataset(security.getUserPrincipal(), targetEntityKey))
    ) {
      machineTag.setCreatedBy(security.getUserPrincipal().getName());
      return addMachineTag(targetEntityKey, machineTag);
    } else {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }
  }

  @Validate(groups = {PrePersist.class, Default.class})
  @Override
  public int addMachineTag(UUID targetEntityKey, @Valid MachineTag machineTag) {
    return WithMyBatis.addMachineTag(machineTagMapper, baseMapper, targetEntityKey, machineTag);
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
  @DELETE
  @Path("{key}/machineTag/{machineTagKey: [0-9]+}")
  @Consumes(MediaType.WILDCARD)
  public void deleteMachineTag(@PathParam("key") UUID targetEntityKey, @PathParam("machineTagKey") int machineTagKey,
                               @Context SecurityContext security) {

    Optional<MachineTag> optMachineTag = WithMyBatis.listMachineTags(baseMapper, targetEntityKey).stream()
      .filter(m -> m.getKey() == machineTagKey).findFirst();

    if (optMachineTag.isPresent()) {
      MachineTag machineTag = optMachineTag.get();

      if (security.isUserInRole(GRSCICOLL_ADMIN_ROLE)
          || userAuthService.allowedToModifyNamespace(security.getUserPrincipal(), machineTag.getNamespace())
          || (security.isUserInRole(GRSCICOLL_EDITOR_ROLE)
              && TagNamespace.GBIF_DEFAULT_TERM.getNamespace().equals(machineTag.getNamespace())
              && userAuthService.allowedToModifyDataset(security.getUserPrincipal(), targetEntityKey))
      ) {
        deleteMachineTag(targetEntityKey, machineTagKey);

      } else {
        throw new WebApplicationException(Response.Status.FORBIDDEN);
      }
    } else {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
  }

  /**
   * Deletes the MachineTag according to interface without security restrictions.
   *
   * @param targetEntityKey key of target entity to delete MachineTag from
   * @param machineTagKey key of MachineTag to delete
   */
  @Override
  public void deleteMachineTag(@PathParam("key") UUID targetEntityKey, @PathParam("machineTagKey") int machineTagKey) {
    WithMyBatis.deleteMachineTag(baseMapper, targetEntityKey, machineTagKey);
  }

  /**
   * The webservice method to delete all machine tag in a namespace.
   * Ensures that the caller is authorized to perform the action by looking at the namespace.
   */
  @DELETE
  @Path("{key}/machineTag/{namespace}")
  @Consumes(MediaType.WILDCARD)
  public void deleteMachineTags(@PathParam("key") UUID targetEntityKey, @PathParam("namespace") String namespace,
                                @Context SecurityContext security) {
    if (!security.isUserInRole(GRSCICOLL_ADMIN_ROLE)
        && !userAuthService.allowedToModifyNamespace(security.getUserPrincipal(), namespace)) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }
    deleteMachineTags(targetEntityKey, namespace);
  }

  @Override
  public void deleteMachineTags(@NotNull UUID targetEntityKey, @NotNull TagNamespace tagNamespace) {
    deleteMachineTags(targetEntityKey, tagNamespace.getNamespace());
  }

  @Override
  public void deleteMachineTags(@NotNull UUID targetEntityKey, @NotNull String namespace) {
    WithMyBatis.deleteMachineTags(baseMapper, targetEntityKey, namespace, null);
  }

  /**
   * The webservice method to delete all machine tag of a particular name in a namespace.
   * Ensures that the caller is authorized to perform the action by looking at the namespace.
   */
  @DELETE
  @Path("{key}/machineTag/{namespace}/{name}")
  @Consumes(MediaType.WILDCARD)
  public void deleteMachineTags(@PathParam("key") UUID targetEntityKey, @PathParam("namespace") String namespace,
                                @PathParam("name") String name, @Context SecurityContext security) {
    if (!security.isUserInRole(GRSCICOLL_ADMIN_ROLE)
        && !userAuthService.allowedToModifyNamespace(security.getUserPrincipal(), namespace)) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }
    deleteMachineTags(targetEntityKey, namespace, name);
  }

  @Override
  public void deleteMachineTags(@NotNull UUID targetEntityKey, @NotNull TagName tagName) {
    deleteMachineTags(targetEntityKey, tagName.getNamespace().getNamespace(), tagName.getName());
  }

  @Override
  public void deleteMachineTags(@NotNull UUID targetEntityKey, @NotNull String namespace, @NotNull String name) {
    WithMyBatis.deleteMachineTags(baseMapper, targetEntityKey, namespace, name);
  }

  @GET
  @Path("{key}/machineTag")
  @Override
  public List<MachineTag> listMachineTags(@PathParam("key") UUID targetEntityKey) {
    return WithMyBatis.listMachineTags(baseMapper, targetEntityKey);
  }

  public PagingResponse<T> listByMachineTag(String namespace, @Nullable String name, @Nullable String value,
                                            @Nullable Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return WithMyBatis.listByMachineTag(baseMapper, namespace, name, value, page);
  }

  abstract void checkUniqueness(T entity);
  abstract void checkUniquenessInUpdate(T oldEntity, T newEntity);
}
