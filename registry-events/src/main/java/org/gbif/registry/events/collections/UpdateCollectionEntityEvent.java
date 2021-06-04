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

/** This event is fired after a new collection entity has been successfully updated. */
public class UpdateCollectionEntityEvent<T extends CollectionEntity>
    extends CollectionsBaseEvent<T> {

  private final T newObject;
  private final T oldObject;

  public static <T extends CollectionEntity> UpdateCollectionEntityEvent<T> newInstance(
      T newObject, T oldObject) {
    return new UpdateCollectionEntityEvent<>(newObject, oldObject, (Class<T>) newObject.getClass());
  }

  private UpdateCollectionEntityEvent(T newObject, T oldObject, Class<T> collectionEntityClass) {
    super(EventType.UPDATE, collectionEntityClass);
    this.newObject = newObject;
    this.oldObject = oldObject;
  }

  public T getNewObject() {
    return newObject;
  }

  public T getOldObject() {
    return oldObject;
  }
}
