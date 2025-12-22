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
package org.gbif.registry.ws.client.collections;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.gbif.api.model.collections.CollectionEntity;

import java.util.UUID;

public interface CrudClient<T extends CollectionEntity> {

  @RequestLine("POST /")
  @Headers("Content-Type: application/json")
  UUID create(T entity);

  @RequestLine("DELETE /{key}")
  void delete(@Param("key") UUID key);

  @RequestLine("GET /{key}")
  @Headers("Accept: application/json")
  T get(@Param("key") UUID key);

  default void update(T entity) {
    updateResource(entity.getKey(), entity);
  }

  @RequestLine("PUT /{key}")
  @Headers("Content-Type: application/json")
  void updateResource(@Param("key") UUID key, T entity);
}
