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
package org.gbif.registry.persistence.mapper.collections;

import org.gbif.api.model.collections.Batch;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/** Mapper for collections-related {@link org.gbif.api.model.collections.Batch}. */
@Repository
public interface BatchMapper {

  void create(Batch batch);

  Batch get(@Param("key") Integer key);

  void update(Batch batch);
}
