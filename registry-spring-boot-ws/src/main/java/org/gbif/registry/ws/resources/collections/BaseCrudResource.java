package org.gbif.registry.ws.resources.collections;

import com.google.common.eventbus.EventBus;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.service.collections.CrudService;
import org.gbif.registry.persistence.CrudMapper;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.constraints.NotNull;
import java.util.UUID;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.GRSCICOLL_ADMIN_ROLE;

/**
 * Base class to implement the CRUD methods of a {@link CollectionEntity}.
 */
public abstract class BaseCrudResource<T extends CollectionEntity> implements CrudService<T> {

  private final CrudMapper<T> crudMapper;
  private final EventBus eventBus;
  private final Class<T> objectClass;

  protected BaseCrudResource(CrudMapper<T> crudMapper, EventBus eventBus, Class<T> objectClass) {
    this.crudMapper = crudMapper;
    this.eventBus = eventBus;
    this.objectClass = objectClass;
  }

  // TODO: 2019-07-24 implement all methods
  // TODO: 2019-07-24 implement @Validate
  // TODO: 2019-07-24 implement @Trim
  @RequestMapping(method = RequestMethod.POST)
//  @Trim
//  @Validate
  @Transactional
  @Secured({ADMIN_ROLE, GRSCICOLL_ADMIN_ROLE})
  public UUID create(@NotNull T entity, Authentication authentication) {
    entity.setCreatedBy(authentication.getName());
    entity.setModifiedBy(authentication.getName());
    return create(entity);
  }
}
