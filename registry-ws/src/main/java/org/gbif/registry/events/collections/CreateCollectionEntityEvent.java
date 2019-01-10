package org.gbif.registry.events.collections;

import org.gbif.api.model.collections.CollectionEntity;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This event is fired after a new collection entity has been successfully created.
 */
public class CreateCollectionEntityEvent<T extends CollectionEntity> {

  private final T newObject;
  private final Class<T> objectClass;

  public static <T extends CollectionEntity> CreateCollectionEntityEvent<T> newInstance(T newObject, Class<T> objectClass) {
    return new CreateCollectionEntityEvent<>(newObject, objectClass);
  }

  private CreateCollectionEntityEvent(T newObject, Class<T> objectClass) {
    this.newObject = checkNotNull(newObject, "newObject can't be null");
    this.objectClass = checkNotNull(objectClass, "objectClass can't be null");
  }

  public T getNewObject() {
    return newObject;
  }

  public Class<T> getObjectClass() {
    return objectClass;
  }

}
