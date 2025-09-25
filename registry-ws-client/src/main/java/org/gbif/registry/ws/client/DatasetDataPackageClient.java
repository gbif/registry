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

import org.gbif.api.annotation.Trim;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.common.paging.PageableBase;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetDataPackageService;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("dataset")
public interface DatasetDataPackageClient extends DatasetDataPackageService {


  @PostMapping(value = "{datasetKey}/datapackage", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  void create(@PathVariable("datasetKey") UUID datasetKey, @RequestBody @Trim Dataset.DataPackage dataPackage);

  @PutMapping(value = "{datasetKey}/datapackage", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  void update(@PathVariable("datasetKey") UUID datasetKey, @RequestBody @Trim Dataset.DataPackage dataPackage);

  @GetMapping(value = "{datasetKey}/datapackage", produces = MediaType.APPLICATION_JSON_VALUE)
  @Override
  Dataset.DataPackage get(@PathVariable("datasetKey") UUID datasetKey);

  @GetMapping(value = "{datasetKey}/datapackage/resource", produces = MediaType.APPLICATION_JSON_VALUE)
  @Override
  String getResources(@PathVariable("datasetKey") UUID datasetKey);

  @GetMapping(value = "{datasetKey}/datapackage/resource/{resourceName}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Override
  String getResource(@PathVariable("datasetKey") UUID datasetKey, @PathVariable("resourceName") String resourceName);

  @GetMapping(value = "{datasetKey}/datapackage/resourceNames", produces = MediaType.APPLICATION_JSON_VALUE)
  @Override
  List<String> getResourceNames(@PathVariable("datasetKey") UUID datasetKey);

  @GetMapping(value = "/datapackage", produces = MediaType.APPLICATION_JSON_VALUE)
  @Override
  PagingResponse<Dataset.DataPackage> list(@SpringQueryMap PageableBase params);

}
