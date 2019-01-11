package org.gbif.registry.ws.resources.collections;

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.service.collections.CrudService;
import org.gbif.registry.events.collections.DeleteCollectionEntityEvent;
import org.gbif.registry.persistence.mapper.collections.CrudMapper;
import org.gbif.registry.ws.guice.Trim;
import org.gbif.ws.server.interceptor.NullToNotFound;

import java.util.UUID;
import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import com.google.common.eventbus.EventBus;
import org.apache.bval.guice.Validate;
import org.mybatis.guice.transactional.Transactional;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.EDITOR_ROLE;

import static com.google.common.base.Preconditions.checkArgument;

/** Base class to implement the CRUD methods of a {@link CollectionEntity}. */
public abstract class BaseCrudResource<T extends CollectionEntity> implements CrudService<T> {

  private final CrudMapper<T> crudMapper;
  private final EventBus eventBus;
  private final Class<T> objectClass;

  protected BaseCrudResource(CrudMapper<T> crudMapper, EventBus eventBus, Class<T> objectClass) {
    this.crudMapper = crudMapper;
    this.eventBus = eventBus;
    this.objectClass = objectClass;
  }

  @POST
  @Trim
  @Validate
  @Transactional
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  public UUID create(@NotNull T entity, @Context SecurityContext security) {
    entity.setCreatedBy(security.getUserPrincipal().getName());
    entity.setModifiedBy(security.getUserPrincipal().getName());
    return create(entity);
  }

  @DELETE
  @Path("{key}")
  @Validate
  @Transactional
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
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
    crudMapper.delete(key);
    eventBus.post(DeleteCollectionEntityEvent.newInstance(objectToDelete, objectClass));
  }

  @GET
  @Path("{key}")
  @Nullable
  @NullToNotFound
  @Validate(validateReturnedValue = true)
  @Override
  public T get(@PathParam("key") @NotNull UUID key) {
    return crudMapper.get(key);
  }

  @PUT
  @Path("{key}")
  @Validate
  @Transactional
  @RolesAllowed({ADMIN_ROLE, EDITOR_ROLE})
  public void update(
      @PathParam("key") @NotNull UUID key,
      @NotNull @Trim T entity,
      @Context SecurityContext security) {
    checkArgument(
        key.equals(entity.getKey()), "Provided entity must have the same key as the resource URL");
    entity.setModifiedBy(security.getUserPrincipal().getName());
    update(entity);
  }
}
