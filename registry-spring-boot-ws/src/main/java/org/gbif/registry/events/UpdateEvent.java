package org.gbif.registry.events;

import org.gbif.api.model.registry.NetworkEntity;

/**
 * This event is fired after a new network entity has been successfully updated.
 */
public class UpdateEvent<T extends NetworkEntity> {

  private final T newObject;
  private final T oldObject;
  private final Class<T> objectClass;

  public static <T extends NetworkEntity> UpdateEvent<T> newInstance(T newObject, T oldObject, Class<T> objectClass) {
    return new UpdateEvent<>(newObject, oldObject, objectClass);
  }

  private UpdateEvent(T newObject, T oldObject, Class<T> objectClass) {
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
