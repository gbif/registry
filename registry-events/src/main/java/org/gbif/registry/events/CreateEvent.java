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
package org.gbif.registry.events;

import com.google.common.base.Preconditions;

/** This event is fired after a new network entity has been successfully created. */
public class CreateEvent<T> {

  private final T newObject;
  private final Class<T> objectClass;

  public static <T> CreateEvent<T> newInstance(T newObject, Class<T> objectClass) {
    return new CreateEvent<>(newObject, objectClass);
  }

  private CreateEvent(T newObject, Class<T> objectClass) {
    this.newObject = Preconditions.checkNotNull(newObject, "newObject can't be null");
    this.objectClass = Preconditions.checkNotNull(objectClass, "objectClass can't be null");
  }

  public T getNewObject() {
    return newObject;
  }

  public Class<T> getObjectClass() {
    return objectClass;
  }
}
