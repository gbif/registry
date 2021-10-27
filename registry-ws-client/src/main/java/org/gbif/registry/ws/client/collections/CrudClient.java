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

import org.gbif.api.model.collections.CollectionEntity;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

public interface CrudClient<T extends CollectionEntity> {

  @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
  UUID create(@RequestBody T entity);

  @RequestMapping(method = RequestMethod.DELETE, value = "{key}")
  void delete(@PathVariable("key") UUID key);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  T get(@PathVariable("key") UUID key);

  default void update(@RequestBody T entity) {
    updateResource(entity.getKey(), entity);
  }

  @RequestMapping(
      method = RequestMethod.PUT,
      value = "{key}",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  void updateResource(@PathVariable("key") UUID key, @RequestBody T entity);
}
