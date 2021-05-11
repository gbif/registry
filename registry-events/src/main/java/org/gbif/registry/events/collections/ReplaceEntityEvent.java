package org.gbif.registry.events.collections;

import org.gbif.api.model.collections.CollectionEntity;

import java.util.UUID;

public class ReplaceEntityEvent<T extends CollectionEntity> extends CollectionsBaseEvent<T> {

  private final UUID targetEntityKey;
  private final UUID replacementKey;

  public static <T extends CollectionEntity> ReplaceEntityEvent<T> newInstance(
      Class<T> collectionEntityClass,
      UUID targetEntityKey,
      UUID replacementKey,
      EventType eventType) {
    return new ReplaceEntityEvent<>(
        collectionEntityClass, targetEntityKey, replacementKey, eventType);
  }

  private ReplaceEntityEvent(
      Class<T> collectionEntityClass,
      UUID targetEntityKey,
      UUID replacementKey,
      EventType eventType) {
    super(eventType, collectionEntityClass);
    this.targetEntityKey = targetEntityKey;
    this.replacementKey = replacementKey;
  }

  public UUID getTargetEntityKey() {
    return targetEntityKey;
  }

  public UUID getReplacementKey() {
    return replacementKey;
  }
}
