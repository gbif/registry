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

/** This event is fired after a new collection entity has been successfully deleted. */
public class DeleteCollectionEntityEvent<T extends CollectionEntity>
    extends CollectionsBaseEvent<T> {

  private final T oldObject;
  private final T deletedObject;

  public static <T extends CollectionEntity> DeleteCollectionEntityEvent<T> newInstance(
      T oldObject, T deletedObject) {
    return new DeleteCollectionEntityEvent<>(
        oldObject, deletedObject, (Class<T>) oldObject.getClass());
  }

  private DeleteCollectionEntityEvent(
      T oldObject, T deletedObject, Class<T> collectionEntityClass) {
    super(EventType.DELETE, collectionEntityClass);
    this.oldObject = oldObject;
    this.deletedObject = deletedObject;
  }

  public T getOldObject() {
    return oldObject;
  }

  public T getDeletedObject() {
    return deletedObject;
  }
}
