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

/** This event is fired after a new network entity has been successfully updated. */
public class UpdateEvent<T> {

  private final T newObject;
  private final T oldObject;
  private final Class<T> objectClass;

  public static <T> UpdateEvent<T> newInstance(T newObject, T oldObject, Class<T> objectClass) {
    return new UpdateEvent<>(newObject, oldObject, objectClass);
  }

  private UpdateEvent(T newObject, T oldObject, Class<T> objectClass) {
    this.newObject = newObject;
    this.oldObject = oldObject;
    this.objectClass = objectClass;
  }

  public T getNewObject() {
    return newObject;
  }

  public T getOldObject() {
    return oldObject;
  }

  public Class<T> getObjectClass() {
    return objectClass;
  }
}
