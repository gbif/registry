package org.gbif.registry.events;

import org.gbif.api.model.registry.NetworkEntity;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This event is fired after a new network entity has been successfully created.
 */
public class CreateEvent<T extends NetworkEntity> {

  private final T newObject;
  private final Class<T> objectClass;

  public static <T extends NetworkEntity> CreateEvent<T> newInstance(T newObject, Class<T> objectClass) {
    return new CreateEvent<>(newObject, objectClass);
  }

  private CreateEvent(T newObject, Class<T> objectClass) {
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
