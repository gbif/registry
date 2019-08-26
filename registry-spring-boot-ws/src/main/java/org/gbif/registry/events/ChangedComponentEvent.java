package org.gbif.registry.events;

import com.google.common.base.Preconditions;

import java.util.UUID;

/**
 * This event is fired after network entity components such as contacts, identifiers, endpoints or tags
 * have been successfully updated, added or removed.
 */
public class ChangedComponentEvent {

  private final UUID targetEntityKey;
  private final Class targetClass;
  private final Class componentClass;

  public static ChangedComponentEvent newInstance(UUID targetEntityKey, Class targetClass, Class componentClass) {
    return new ChangedComponentEvent(targetEntityKey, targetClass, componentClass);
  }

  private ChangedComponentEvent(UUID targetEntityKey, Class targetClass, Class componentClass) {
    this.targetEntityKey = Preconditions.checkNotNull(targetEntityKey);
    this.targetClass = Preconditions.checkNotNull(targetClass);
    this.componentClass = Preconditions.checkNotNull(componentClass);
  }

  public UUID getTargetEntityKey() {
    return targetEntityKey;
  }

  public Class getTargetClass() {
    return targetClass;
  }

  public Class getComponentClass() {
    return componentClass;
  }
}
