package org.gbif.registry.service.collections.audit;

import org.gbif.api.model.collections.CollectionEntity;

public class UpdatedEntityEvent<T extends CollectionEntity> extends BaseEvent {

  private final T oldEntity;
  private final T newEntity;

  public UpdatedEntityEvent(T oldEntity, T newEntity) {
    this.oldEntity = oldEntity;
    this.newEntity = newEntity;
    this.entityType = getEntityType(oldEntity);
    this.note = "Update " + newEntity.getClass().getSimpleName();
  }

  public T getOldEntity() {
    return oldEntity;
  }

  public T getNewEntity() {
    return newEntity;
  }
}
