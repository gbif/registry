/*
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

import com.google.common.base.Preconditions;

/**
 * This event is fired after collection entity components such as contacts, identifiers or tags have
 * been successfully updated, added or removed.
 */
public class SubEntityCollectionEvent<T extends CollectionEntity, R>
    extends CollectionsBaseEvent<T> {

  private final UUID collectionEntityKey;
  private final Class<R> subEntityClass;
  // it's a string so we can use uuids and integer keys
  private final String subEntityKey;
  private final R subEntity;
  // only applies for subentities that can be updated like ChangeSuggestion
  private R oldSubEntity;

  public static <T extends CollectionEntity, R> SubEntityCollectionEvent<T, R> newInstance(
      UUID collectionEntityKey,
      Class<T> collectionEntityClass,
      Class<R> subEntityClass,
      UUID subEntityKey,
      EventType eventType) {
    return new SubEntityCollectionEvent<>(
        collectionEntityKey,
        collectionEntityClass,
        subEntityClass,
        null,
        null,
        subEntityKey.toString(),
        eventType);
  }

  public static <T extends CollectionEntity, R> SubEntityCollectionEvent<T, R> newInstance(
      UUID collectionEntityKey,
      Class<T> collectionEntityClass,
      R subEntity,
      int subEntityKey,
      EventType eventType) {
    return new SubEntityCollectionEvent<>(
        collectionEntityKey,
        collectionEntityClass,
        (Class<R>) subEntity.getClass(),
        subEntity,
        null,
        String.valueOf(subEntityKey),
        eventType);
  }

  public static <T extends CollectionEntity, R> SubEntityCollectionEvent<T, R> newInstance(
      UUID collectionEntityKey,
      Class<T> collectionEntityClass,
      R subEntity,
      R oldSubEntity,
      int subEntityKey,
      EventType eventType) {
    return new SubEntityCollectionEvent<>(
        collectionEntityKey,
        collectionEntityClass,
        (Class<R>) subEntity.getClass(),
        subEntity,
        oldSubEntity,
        String.valueOf(subEntityKey),
        eventType);
  }

  private SubEntityCollectionEvent(
      UUID collectionEntityKey,
      Class<T> collectionEntityClass,
      Class<R> subEntityClass,
      R subEntity,
      R oldSubEntity,
      String subEntityKey,
      EventType eventType) {
    super(eventType, collectionEntityClass);
    this.collectionEntityKey = collectionEntityKey;
    this.subEntityClass = Preconditions.checkNotNull(subEntityClass);
    this.subEntity = subEntity;
    this.subEntityKey = Preconditions.checkNotNull(subEntityKey);
    this.oldSubEntity = oldSubEntity;
  }

  public UUID getCollectionEntityKey() {
    return collectionEntityKey;
  }

  public Class<R> getSubEntityClass() {
    return subEntityClass;
  }

  public R getSubEntity() {
    return subEntity;
  }

  public R getOldSubEntity() {
    return oldSubEntity;
  }

  public String getSubEntityKey() {
    return subEntityKey;
  }
}
