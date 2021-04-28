package org.gbif.registry.service.collections.audit;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.EntityType;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;

abstract class BaseEvent {

  protected String note;
  protected EntityType entityType;

  public String getNote() {
    return note;
  }

  public EntityType getEntityType() {
    return entityType;
  }

  protected <T extends CollectionEntity> EntityType getEntityType(T entity) {
    if (entity instanceof Institution) {
      return EntityType.INSTITUTION;
    } else if (entity instanceof Collection) {
      return EntityType.COLLECTION;
    } else if (entity instanceof Person) {
      return EntityType.PERSON;
    }

    throw new IllegalArgumentException(
        "Entity type not supported: " + entity.getClass().getSimpleName());
  }
}
