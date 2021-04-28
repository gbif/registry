package org.gbif.registry.service.collections.audit;

import org.gbif.api.model.collections.CollectionEntity;

public class CreatedEntityEvent<T extends CollectionEntity> extends BaseEvent {

  private final T createdEntity;

  public CreatedEntityEvent(T createdEntity) {
    this.createdEntity = createdEntity;
    this.entityType = getEntityType(createdEntity);
    this.note = "Create " + createdEntity.getClass().getSimpleName();
  }

  public T getCreatedEntity() {
    return createdEntity;
  }
}
