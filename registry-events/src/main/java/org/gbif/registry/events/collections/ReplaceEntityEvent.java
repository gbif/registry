/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
