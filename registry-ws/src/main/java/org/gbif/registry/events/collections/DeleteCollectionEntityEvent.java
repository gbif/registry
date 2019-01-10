package org.gbif.registry.events.collections;

import org.gbif.api.model.collections.CollectionEntity;

/**
 * This event is fired after a new collection entity has been successfully deleted.
 */
public class DeleteCollectionEntityEvent<T extends CollectionEntity> {

  private final T oldObject;
  private final Class<T> objectClass;

  public static <T extends CollectionEntity> DeleteCollectionEntityEvent<T> newInstance(T oldObject, Class<T> objectClass) {
    return new DeleteCollectionEntityEvent<>(oldObject, objectClass);
  }

  private DeleteCollectionEntityEvent(T oldObject, Class<T> objectClass) {
    this.oldObject = oldObject;
    this.objectClass = objectClass;
  }

  public T getOldObject() {
    return oldObject;
  }

  public Class<T> getObjectClass() {
    return objectClass;
  }

}
