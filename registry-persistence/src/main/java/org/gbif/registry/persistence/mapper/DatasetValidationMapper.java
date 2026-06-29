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
package org.gbif.registry.persistence.mapper;

import java.util.UUID;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DatasetValidationMapper {

  void createOrUpdate(
      @Param("datasetKey") UUID datasetKey,
      @Param("attempt") int attempt,
      @Param("report") String report);

  String get(
      @Param("datasetKey") UUID datasetKey,
      @Param("attempt") int attempt);

  String getLatest(@Param("datasetKey") UUID datasetKey);
}
