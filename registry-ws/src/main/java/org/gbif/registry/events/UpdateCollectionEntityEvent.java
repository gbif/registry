package org.gbif.registry.events;

import org.gbif.api.model.collections.CollectionEntity;

/**
 * This event is fired after a new collection entity has been successfully updated.
 */
public class UpdateCollectionEntityEvent<T extends CollectionEntity> {

  private final T newObject;
  private final T oldObject;
  private final Class<T> objectClass;

  public static <T extends CollectionEntity> UpdateCollectionEntityEvent<T> newInstance(T newObject, T oldObject, Class<T> objectClass) {
    return new UpdateCollectionEntityEvent<>(newObject, oldObject, objectClass);
  }

  private UpdateCollectionEntityEvent(T newObject, T oldObject, Class<T> objectClass) {
    this.newObject = newObject;
    this.oldObject = oldObject;
    this.objectClass = objectClass;
  }

  public T getNewObject() {
    return newObject;
  }

  public T getOldObject() {
    return oldObject;
  }

  public Class<T> getObjectClass() {
    return objectClass;
  }

}
