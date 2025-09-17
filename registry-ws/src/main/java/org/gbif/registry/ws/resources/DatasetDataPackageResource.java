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
package org.gbif.registry.ws.resources;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;

import lombok.extern.slf4j.Slf4j;

import org.gbif.api.annotation.Experimental;
import org.gbif.api.annotation.Trim;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetDataPackageService;
import org.gbif.registry.persistence.mapper.*;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;

@SuppressWarnings("UnstableApiUsage")
@io.swagger.v3.oas.annotations.tags.Tag(
    name = "Dataset Data Package",
    description =
        "APIs for managing Dataset Data Package information. Requires ADMIN role.",
    extensions =
        @Extension(
            name = "Order",
            properties = @ExtensionProperty(name = "Order", value = "0100")))
@Validated
@Primary
@RestController
@RequestMapping(value = "dataset", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
@Experimental
public class DatasetDataPackageResource implements DatasetDataPackageService {

  private final DatasetDataPackageMapper datasetDataPackageMapper;

  public DatasetDataPackageResource(DatasetDataPackageMapper datasetDataPackageMapper) {
    this.datasetDataPackageMapper = datasetDataPackageMapper;
  }


  @Hidden
  @PostMapping(value = "{datasetKey}/datapackage", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Transactional
  @Secured(ADMIN_ROLE)
  @Override
  public void createDataPackageData(@PathVariable("datasetKey") UUID datasetKey, @RequestBody @Trim Dataset.DataPackage dataPackage) {
    datasetDataPackageMapper.createDataPackageDataset(datasetKey, dataPackage);
  }

  @Hidden
  @PutMapping(value = "{datasetKey}/datapackage", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Transactional
  @Secured(ADMIN_ROLE)
  @Override
  public void updateDataPackageData(@PathVariable("datasetKey") UUID datasetKey, @RequestBody @Trim Dataset.DataPackage dataPackage) {
    datasetDataPackageMapper.updateDataPackageDataset(datasetKey, dataPackage);
  }

  @Hidden
  @GetMapping(value = "{datasetKey}/datapackage", produces = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Override
  public Dataset.DataPackage getDataPackageData(@PathVariable("datasetKey") UUID datasetKey) {
    return datasetDataPackageMapper.getDataPackageDataset(datasetKey);
  }
}
