package org.gbif.registry.events.collections;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;

abstract class CollectionsBaseEvent<T extends CollectionEntity> {

  protected final Class<T> collectionEntityClass;
  protected final EventType eventType;

  protected CollectionsBaseEvent(EventType eventType, Class<T> collectionEntityClass) {
    this.eventType = eventType;
    this.collectionEntityClass = collectionEntityClass;
  }

  public Class<T> getCollectionEntityClass() {
    return collectionEntityClass;
  }

  public EventType getEventType() {
    return eventType;
  }

  public CollectionEntityType getCollectionEntityType() {
    if (collectionEntityClass.equals(Institution.class)) {
      return CollectionEntityType.INSTITUTION;
    } else if (collectionEntityClass.equals(Collection.class)) {
      return CollectionEntityType.COLLECTION;
    } else if (collectionEntityClass.equals(Person.class)) {
      return CollectionEntityType.PERSON;
    }

    throw new IllegalArgumentException(
        "Entity type not supported: " + collectionEntityClass.getSimpleName());
  }
}
