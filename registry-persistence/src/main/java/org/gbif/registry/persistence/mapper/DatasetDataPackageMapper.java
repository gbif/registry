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

/**
 * Mapper for operations on the dataset data package (a JSONB column in the dataset table).
 */
@Repository
public interface DatasetDataPackageMapper {

  /**
   * Inserts a new dataset data package record.
   *
   * @param datasetKey the UUID of the dataset
   * @param dataPackage the data package to insert
   */
  void create(@Param("datasetKey") UUID datasetKey, @Param("dp") Dataset.DataPackage dataPackage);

  /**
   * Updates an existing dataset data package record.
   *
   * @param datasetKey the UUID of the dataset
   * @param dataPackage the data package to update
   */
  void update(@Param("datasetKey") UUID datasetKey, @Param("dp") Dataset.DataPackage dataPackage);

  /**
   * Retrieves the data package associated with a dataset.
   *
   * @param datasetKey the UUID of the dataset
   * @return the data package of the dataset, or null if not found
   */
  Dataset.DataPackage get(@Param("datasetKey") UUID datasetKey);

  /**
   *  Retrieves the data package resources associated with a dataset.
   * @param datasetKey the UUID of the dataset
   * @return a String containing a JSON array with the resources(schemas) of data package
   */
  String getResources(@Param("datasetKey") UUID datasetKey);

  /**
   *  Retrieves the data package resources by their name.
   * @param datasetKey the UUID of the dataset
   * @param resourceName data package resource unique name
   * @return a String containing a JSON object with the resources(schema) of the data package
   */
  String getResource(@Param("datasetKey") UUID datasetKey, @Param("resourceName") String resourceName);

  /**
   *  Retrieves the data package resource field values.
   * @param datasetKey the UUID of the dataset
   * @param resourceNameFieldName resourceNameFieldName to retrieve
   * @return a String containing a JSON object with the resources(schema) of the data package
   */
  String getResourceField(@Param("datasetKey") UUID datasetKey, @Param("resourceNameFieldName") String resourceNameFieldName);

  List<Dataset.DataPackage> list(@Param("params") PageableBase params);

  Long count();

}
