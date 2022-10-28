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

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Institution;

abstract class CollectionsBaseEvent<T extends CollectionEntity> {

  protected final Class<T> collectionEntityClass;
  protected final EventType eventType;

  protected CollectionsBaseEvent(EventType eventType, Class<T> collectionEntityClass) {
    this.eventType = eventType;
    this.collectionEntityClass = collectionEntityClass;
  }

  public Class<T> getCollectionEntityClass() {
    return collectionEntityClass;
  }

  public EventType getEventType() {
    return eventType;
  }

  public CollectionEntityType getCollectionEntityType() {
    if (collectionEntityClass.equals(Institution.class)) {
      return CollectionEntityType.INSTITUTION;
    } else if (collectionEntityClass.equals(Collection.class)) {
      return CollectionEntityType.COLLECTION;
    }

    throw new IllegalArgumentException(
        "Entity type not supported: " + collectionEntityClass.getSimpleName());
  }
}
