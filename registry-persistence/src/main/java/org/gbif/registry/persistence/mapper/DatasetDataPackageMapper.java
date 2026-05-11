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

import org.apache.ibatis.annotations.Param;

import org.gbif.api.model.common.paging.PageableBase;
import org.gbif.api.model.registry.Dataset;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DatasetDataPackageMapper {

  void create(
      @Param("datasetKey") UUID datasetKey,
      @Param("dp") Dataset.DataPackage dataPackage,
      @Param("user") String user);

  void update(
      @Param("datasetKey") UUID datasetKey,
      @Param("dp") Dataset.DataPackage dataPackage,
      @Param("user") String user);

  Dataset.DataPackage get(@Param("datasetKey") UUID datasetKey);

  String getResources(@Param("datasetKey") UUID datasetKey);

  String getResource(@Param("datasetKey") UUID datasetKey, @Param("resourceName") String resourceName);

  String getResourceField(
      @Param("datasetKey") UUID datasetKey, @Param("resourceNameFieldName") String resourceNameFieldName);

  List<Dataset.DataPackage> list(@Param("params") PageableBase params);

  Long count();
}
