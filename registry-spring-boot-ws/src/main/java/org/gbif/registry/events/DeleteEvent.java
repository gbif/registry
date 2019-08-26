package org.gbif.registry.events;

import org.gbif.api.model.registry.NetworkEntity;

/**
 * This event is fired after a new network entity has been successfully deleted.
 */
public class DeleteEvent<T extends NetworkEntity> {

  private final T oldObject;
  private final Class<T> objectClass;

  public static <T extends NetworkEntity> DeleteEvent<T> newInstance(T oldObject, Class<T> objectClass) {
    return new DeleteEvent<>(oldObject, objectClass);
  }

  private DeleteEvent(T oldObject, Class<T> objectClass) {
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
