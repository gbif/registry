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
package org.gbif.registry.ws.client;
import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.common.paging.PageableBase;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetDataPackageService;

import java.util.List;
import java.util.UUID;

public interface DatasetDataPackageClient extends DatasetDataPackageService {

  // ---------------------------------------------------------------------------
  // Create / update
  // ---------------------------------------------------------------------------

  @Override
  @RequestLine("POST /dataset/{datasetKey}/datapackage")
  @Headers("Content-Type: application/json")
  void create(
    @Param("datasetKey") UUID datasetKey,
    Dataset.DataPackage dataPackage);

  @Override
  @RequestLine("PUT /dataset/{datasetKey}/datapackage")
  @Headers("Content-Type: application/json")
  void update(
    @Param("datasetKey") UUID datasetKey,
    Dataset.DataPackage dataPackage);

  // ---------------------------------------------------------------------------
  // Retrieve datapackage
  // ---------------------------------------------------------------------------

  @Override
  @RequestLine("GET /dataset/{datasetKey}/datapackage")
  @Headers("Accept: application/json")
  Dataset.DataPackage get(
    @Param("datasetKey") UUID datasetKey);

  // ---------------------------------------------------------------------------
  // Resources
  // ---------------------------------------------------------------------------

  @Override
  @RequestLine("GET /dataset/{datasetKey}/datapackage/resource")
  @Headers("Accept: application/json")
  String getResources(
    @Param("datasetKey") UUID datasetKey);

  @Override
  @RequestLine("GET /dataset/{datasetKey}/datapackage/resource/{resourceName}")
  @Headers("Accept: application/json")
  String getResource(
    @Param("datasetKey") UUID datasetKey,
    @Param("resourceName") String resourceName);

  @Override
  @RequestLine("GET /dataset/{datasetKey}/datapackage/resourceNames")
  @Headers("Accept: application/json")
  List<String> getResourceNames(
    @Param("datasetKey") UUID datasetKey);

  // ---------------------------------------------------------------------------
  // Listing
  // ---------------------------------------------------------------------------

  @Override
  @RequestLine("GET /dataset/datapackage")
  @Headers("Accept: application/json")
  PagingResponse<Dataset.DataPackage> list(
    @QueryMap PageableBase params);
}

