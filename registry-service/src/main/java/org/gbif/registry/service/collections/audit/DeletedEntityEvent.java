package org.gbif.registry.service.collections.audit;

import org.gbif.api.model.collections.CollectionEntity;

public class DeletedEntityEvent<T extends CollectionEntity> extends BaseEvent {

  private final T deletedEntity;

  public DeletedEntityEvent(T deletedEntity) {
    this.deletedEntity = deletedEntity;
    this.entityType = getEntityType(deletedEntity);
    this.note = "Delete " + deletedEntity.getClass().getSimpleName();
  }

  public T getDeletedEntity() {
    return deletedEntity;
  }
}
